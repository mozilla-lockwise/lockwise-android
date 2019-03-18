/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.AppWebPageFragmentArgs
import mozilla.lockbox.view.DialogFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemDetailFragmentArgs

@ExperimentalCoroutinesApi
class RoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : Presenter() {
    private lateinit var navController: NavController
    private val backListener = OnBackPressedCallback {
        dispatcher.dispatch(RouteAction.InternalBack)
        false
    }

    private val navHostFragmentManager: FragmentManager
        get() {
            val fragmentManager = activity.supportFragmentManager
            val navHost = fragmentManager.fragments.last()
            return navHost.childFragmentManager
        }

    private val currentFragment: Fragment
        get() {
            return navHostFragmentManager.fragments.last()
        }

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.fragment_nav_host)
    }

    override fun onPause() {
        super.onPause()
        activity.removeOnBackPressedCallback(backListener)
        compositeDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        activity.addOnBackPressedCallback(backListener)
        routeStore.routes
            .observeOn(mainThread())
            .subscribe(this::route)
            .addTo(compositeDisposable)
    }

    private fun route(action: RouteAction) {
        activity.setTheme(R.style.AppTheme)
        when (action) {
            is RouteAction.Welcome -> navigateToFragment(R.id.fragment_welcome)
            is RouteAction.Login -> navigateToFragment(R.id.fragment_fxa_login)
            is RouteAction.Onboarding.FingerprintAuth ->
                navigateToFragment(R.id.fragment_fingerprint_onboarding)
            is RouteAction.Onboarding.Autofill -> navigateToFragment(R.id.fragment_autofill_onboarding)
            is RouteAction.Onboarding.Confirmation -> navigateToFragment(R.id.fragment_onboarding_confirmation)
            is RouteAction.ItemList -> navigateToFragment(R.id.fragment_item_list)
            is RouteAction.SettingList -> navigateToFragment(R.id.fragment_setting)
            is RouteAction.AccountSetting -> navigateToFragment(R.id.fragment_account_setting)
            is RouteAction.LockScreen -> navigateToFragment(R.id.fragment_locked)
            is RouteAction.Filter -> navigateToFragment(R.id.fragment_filter)
            is RouteAction.ItemDetail -> navigateToFragment(R.id.fragment_item_detail, bundle(action))
            is RouteAction.OpenWebsite -> openWebsite(action.url)
            is RouteAction.SystemSetting -> openSetting(action)
            is RouteAction.UnlockFallbackDialog -> showUnlockFallback(action)
            is RouteAction.AutoLockSetting -> showAutoLockSelections()
            is RouteAction.DialogFragment.FingerprintDialog ->
                showDialogFragment(FingerprintAuthDialogFragment(), action)
            is DialogAction -> showDialog(action)
            is AppWebPageAction -> navigateToFragment(R.id.fragment_webview, bundle(action))
        }
    }

    private fun bundle(action: AppWebPageAction): Bundle {
        return AppWebPageFragmentArgs.Builder()
            .setUrl(action.url!!)
            .setTitle(action.title!!)
            .build()
            .toBundle()
    }

    private fun bundle(action: RouteAction.ItemDetail): Bundle {
        return ItemDetailFragmentArgs.Builder()
            .setItemId(action.id)
            .build()
            .toBundle()
    }

    private fun showDialog(destination: DialogAction) {
        AlertDialogHelper.showAlertDialog(activity, destination.viewModel)
            .map { alertState ->
                when (alertState) {
                    AlertState.BUTTON_POSITIVE -> {
                        destination.positiveButtonActionList
                    }
                    AlertState.BUTTON_NEGATIVE -> {
                        destination.negativeButtonActionList
                    }
                }
            }
            .flatMapIterable { listOf(RouteAction.InternalBack) + it }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun showAutoLockSelections() {
        val autoLockValues = Setting.AutoLockTime.values()
        val items = autoLockValues.map { it.stringValue }.toTypedArray()

        settingStore.autoLockTime.take(1)
            .map { autoLockValues.indexOf(it) }
            .flatMap {
                AlertDialogHelper.showRadioAlertDialog(
                    activity,
                    R.string.auto_lock,
                    items,
                    it,
                    negativeButtonTitle = R.string.cancel
                )
            }
            .flatMapIterable {
                listOf(RouteAction.InternalBack, SettingAction.AutoLockTime(autoLockValues[it]))
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun showDialogFragment(dialogFragment: DialogFragment, destination: RouteAction.DialogFragment) {
        try {
            dialogFragment.setTargetFragment(currentFragment, 0)
            dialogFragment.show(navHostFragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }

    private fun showUnlockFallback(action: RouteAction.UnlockFallbackDialog) {
        val manager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = manager.createConfirmDeviceCredentialIntent(
            activity.getString(R.string.unlock_fallback_title),
            activity.getString(R.string.confirm_pattern)
        )
        currentFragment.startActivityForResult(intent, action.requestCode)
    }

    private fun navigateToFragment(@IdRes destinationId: Int, args: Bundle? = null) {
        val src = navController.currentDestination ?: return
        val srcId = src.id
        if (srcId == destinationId && args == null) {
            // No point in navigating if nothing has changed.
            return
        }

        val transition = findTransitionId(srcId, destinationId) ?: destinationId

        val navOptions = if (transition == destinationId) {
            // Without being able to detect if we're in developer mode,
            // it is too dangerous to RuntimeException.
            val from = activity.resources.getResourceName(srcId)
            val to = activity.resources.getResourceName(destinationId)
            val graphName = activity.resources.getResourceName(navController.graph.id)
            log.error(
                "Cannot route from $from to $to. " +
                    "This is a developer bug, fixable by adding an action to $graphName.xml and/or ${javaClass.simpleName}"
            )
            null
        } else {
            // Get the transition action out of the graph, before we manually clear the back
            // stack, because it causes IllegalArgumentExceptions.
            src.getAction(transition)?.navOptions?.let { navOptions ->
                if (navOptions.shouldLaunchSingleTop()) {
                    while (navController.popBackStack()) {
                        // NOP
                    }
                    routeStore.clearBackStack()
                }
                navOptions
            }
        }

        try {
            navController.navigate(destinationId, args, navOptions)
        } catch (e: Throwable) {
            log.error("This appears to be a bug in navController", e)
            navController.navigate(destinationId, args)
        }
    }

    private fun findTransitionId(@IdRes from: Int, @IdRes to: Int): Int? {
        // This maps two nodes in the graph_main.xml to the edge between them.
        // If a RouteAction is called from a place the graph doesn't know about then
        // the app will log.error.
        return when (from to to) {
            R.id.fragment_null to R.id.fragment_item_list -> R.id.action_init_to_unlocked
            R.id.fragment_null to R.id.fragment_locked -> R.id.action_init_to_locked
            R.id.fragment_null to R.id.fragment_welcome -> R.id.action_init_to_unprepared

            R.id.fragment_welcome to R.id.fragment_fxa_login -> R.id.action_welcome_to_fxaLogin

            R.id.fragment_fxa_login to R.id.fragment_item_list -> R.id.action_fxaLogin_to_itemList
            R.id.fragment_fxa_login to R.id.fragment_fingerprint_onboarding ->
                R.id.action_fxaLogin_to_fingerprint_onboarding
            R.id.fragment_fxa_login to R.id.fragment_onboarding_confirmation ->
                R.id.action_fxaLogin_to_onboarding_confirmation

            R.id.fragment_fingerprint_onboarding to R.id.fragment_onboarding_confirmation ->
                R.id.action_fingerprint_onboarding_to_confirmation
            R.id.fragment_fingerprint_onboarding to R.id.fragment_autofill_onboarding ->
                R.id.action_onboarding_fingerprint_to_autofill

            R.id.fragment_autofill_onboarding to R.id.fragment_item_list -> R.id.action_to_itemList
            R.id.fragment_autofill_onboarding to R.id.fragment_onboarding_confirmation -> R.id.action_autofill_onboarding_to_confirmation

            R.id.fragment_onboarding_confirmation to R.id.fragment_item_list -> R.id.action_to_itemList
            R.id.fragment_onboarding_confirmation to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_locked to R.id.fragment_item_list -> R.id.action_to_itemList
            R.id.fragment_locked to R.id.fragment_welcome -> R.id.action_locked_to_welcome

            R.id.fragment_item_list to R.id.fragment_item_detail -> R.id.action_itemList_to_itemDetail
            R.id.fragment_item_list to R.id.fragment_setting -> R.id.action_itemList_to_setting
            R.id.fragment_item_list to R.id.fragment_account_setting -> R.id.action_itemList_to_accountSetting
            R.id.fragment_item_list to R.id.fragment_locked -> R.id.action_to_locked
            R.id.fragment_item_list to R.id.fragment_filter -> R.id.action_itemList_to_filter
            R.id.fragment_item_list to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_item_detail to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_setting to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_account_setting to R.id.fragment_welcome -> R.id.action_to_welcome

            R.id.fragment_filter to R.id.fragment_item_detail -> R.id.action_filter_to_itemDetail

            else -> null
        }
    }

    private fun openWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(browserIntent, null)
    }

    private fun openSetting(settingAction: RouteAction.SystemSetting) {
        val settingIntent = Intent(settingAction.setting.intentAction)
        settingIntent.data = settingAction.setting.data
        activity.startActivity(settingIntent, null)
    }
}
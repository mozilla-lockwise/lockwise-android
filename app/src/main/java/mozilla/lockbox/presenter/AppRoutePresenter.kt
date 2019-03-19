/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.reactivex.android.schedulers.AndroidSchedulers
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
import mozilla.lockbox.log
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.AppWebPageFragmentArgs
import mozilla.lockbox.view.DialogFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemDetailFragmentArgs

@ExperimentalCoroutinesApi
class AppRoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : RoutePresenter(activity) {

    private lateinit var navController: NavController

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.fragment_nav_host)
    }

    override fun onPause() {
        super.onPause()

        compositeDisposable.clear()
    }

    override fun onResume() {
        super.onResume()

        routeStore.routes
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::route)
            .addTo(compositeDisposable)
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> navigateTo(R.id.fragment_welcome)
            is RouteAction.Login -> navigateTo(R.id.fragment_fxa_login)
            is RouteAction.Onboarding.FingerprintAuth ->
                navigateTo(R.id.fragment_fingerprint_onboarding)
            is RouteAction.Onboarding.Autofill -> navigateTo(R.id.fragment_autofill_onboarding)
            is RouteAction.Onboarding.Confirmation -> navigateTo(R.id.fragment_onboarding_confirmation)
            is RouteAction.ItemList -> navigateTo(R.id.fragment_item_list)
            is RouteAction.SettingList -> navigateTo(R.id.fragment_setting)
            is RouteAction.AccountSetting -> navigateTo(R.id.fragment_account_setting)
            is RouteAction.LockScreen -> navigateTo(R.id.fragment_locked)
            is RouteAction.Filter -> navigateTo(R.id.fragment_filter)
            is RouteAction.ItemDetail -> navigateTo(R.id.fragment_item_detail, bundle(action))
            is RouteAction.OpenWebsite -> openWebsite(action.url)
            is RouteAction.SystemSetting -> openSetting(action)
            is RouteAction.AutoLockSetting -> showAutoLockSelections()
            is RouteAction.DialogFragment.FingerprintDialog ->
                showDialogFragment(FingerprintAuthDialogFragment(), action)
            is DialogAction -> showDialog(action)
            is AppWebPageAction -> navigateTo(R.id.fragment_webview, bundle(action))
        }
    }

    private fun navigateTo(@IdRes destinationId: Int, args: Bundle? = null) {
        super.navigateToFragment(navController, destinationId, args)
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
            .flatMapIterable { it }
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
            .map {
                autoLockValues[it]
            }
            .subscribe {
                dispatcher.dispatch(SettingAction.AutoLockTime(it))
            }
            .addTo(compositeDisposable)
    }

    // could possibly put this in RoutePresenter? slight differences between these versions
    private fun showDialogFragment(dialogFragment: DialogFragment?, destination: RouteAction.DialogFragment) {
        if (dialogFragment != null) {
            val fragmentManager = activity.supportFragmentManager
            try {
                dialogFragment.setTargetFragment(fragmentManager.fragments.last(), 0)
                dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
                dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
            } catch (e: IllegalStateException) {
                log.error("Could not show dialog", e)
            }
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
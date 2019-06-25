/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
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
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.AppWebPageFragmentArgs
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemDetailFragmentArgs

@ExperimentalCoroutinesApi
class AppRoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : RoutePresenter(activity, dispatcher, routeStore) {

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
            .observeOn(mainThread())
            .subscribe(this::route)
            .addTo(compositeDisposable)
    }

    fun bundle(action: AppWebPageAction): Bundle {
        return AppWebPageFragmentArgs.Builder()
            .setUrl(action.url!!)
            .setTitle(action.title!!)
            .build()
            .toBundle()
    }

    fun bundle(action: RouteAction.ItemDetail): Bundle {
        return ItemDetailFragmentArgs.Builder()
            .setItemId(action.id)
            .build()
            .toBundle()
    }

    override fun route(action: RouteAction) {
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

    override fun findTransitionId(@IdRes src: Int, @IdRes dest: Int): Int? {
        // This maps two nodes in the graph_main.xml to the edge between them.
        // If a RouteAction is called from a place the graph doesn't know about then
        // the app will log.error.
        return when (src to dest) {
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

            R.id.fragment_locked to R.id.fragment_item_list -> R.id.action_locked_to_itemList
            R.id.fragment_locked to R.id.fragment_welcome -> R.id.action_locked_to_welcome

            R.id.fragment_item_list to R.id.fragment_item_detail -> R.id.action_itemList_to_itemDetail
            R.id.fragment_item_list to R.id.fragment_setting -> R.id.action_itemList_to_setting
            R.id.fragment_item_list to R.id.fragment_account_setting -> R.id.action_itemList_to_accountSetting
            R.id.fragment_item_list to R.id.fragment_locked -> R.id.action_itemList_to_locked
            R.id.fragment_item_list to R.id.fragment_filter -> R.id.action_itemList_to_filter
            R.id.fragment_item_list to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_item_detail to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_setting to R.id.fragment_webview -> R.id.action_to_webview

            R.id.fragment_account_setting to R.id.fragment_welcome -> R.id.action_to_welcome

            R.id.fragment_filter_backdrop to R.id.fragment_item_detail -> R.id.action_filter_to_itemDetail

            else -> null
        }
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
}
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
import mozilla.lockbox.view.FingerprintAuthDialogFragment

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


    private fun showUnlockFallback(action: RouteAction.UnlockFallbackDialog) {
        val manager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = manager.createConfirmDeviceCredentialIntent(
            activity.getString(R.string.unlock_fallback_title),
            activity.getString(R.string.confirm_pattern)
        )
        currentFragment.startActivityForResult(intent, action.requestCode)
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
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
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.RoutePresenterSupport
import mozilla.lockbox.view.AppWebPageFragmentArgs
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemDetailFragmentArgs

@ExperimentalCoroutinesApi
class RoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val routePresenterSupport: RoutePresenterSupport = RoutePresenterSupport()
) : Presenter() {
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
            .observeOn(mainThread())
            .subscribe(this::route)
            .addTo(compositeDisposable)
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_welcome, this::findTransitionId)
            is RouteAction.Login -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_fxa_login, this::findTransitionId)
            is RouteAction.Onboarding.FingerprintAuth ->
                routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_fingerprint_onboarding, this::findTransitionId)
            is RouteAction.Onboarding.Autofill -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_autofill_onboarding, this::findTransitionId)
            is RouteAction.Onboarding.Confirmation -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_onboarding_confirmation, this::findTransitionId)
            is RouteAction.ItemList -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_item_list, this::findTransitionId)
            is RouteAction.SettingList -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_setting, this::findTransitionId)
            is RouteAction.AccountSetting -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_account_setting, this::findTransitionId)
            is RouteAction.LockScreen -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_locked, this::findTransitionId)
            is RouteAction.Filter -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_filter, this::findTransitionId)
            is RouteAction.ItemDetail -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_item_detail, this::findTransitionId, bundle(action))
            is RouteAction.OpenWebsite -> openWebsite(action.url)
            is RouteAction.SystemSetting -> openSetting(action)
            is RouteAction.AutoLockSetting -> showAutoLockSelections()
            is RouteAction.DialogFragment.FingerprintDialog -> routePresenterSupport.showDialogFragment(activity, FingerprintAuthDialogFragment(), action)
            is DialogAction -> showDialog(action)
            is AppWebPageAction -> routePresenterSupport.navigateToFragment(navController, activity, R.id.fragment_webview, this::findTransitionId, bundle(action))
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

    private fun findTransitionId(@IdRes from: Int, @IdRes to: Int): Int? {
        // This maps two nodes in the graph_main.xml to the edge between them.
        // If a RouteAction is called from a place the graph doesn't know about then
        // the app will log.error.
        return when (Pair(from, to)) {
            Pair(R.id.fragment_welcome, R.id.fragment_item_list) -> R.id.action_to_itemList
            Pair(R.id.fragment_welcome, R.id.fragment_item_list) -> R.id.action_to_webview
            Pair(R.id.fragment_welcome, R.id.fragment_locked) -> R.id.action_to_locked

            Pair(R.id.fragment_fxa_login, R.id.fragment_item_list) -> R.id.action_fxaLogin_to_itemList
            Pair(R.id.fragment_fxa_login, R.id.fragment_fingerprint_onboarding) ->
                R.id.action_fxaLogin_to_fingerprint_onboarding
            Pair(R.id.fragment_fxa_login, R.id.fragment_onboarding_confirmation) ->
                R.id.action_fxaLogin_to_onboarding_confirmation

            Pair(R.id.fragment_fingerprint_onboarding, R.id.fragment_onboarding_confirmation) ->
                R.id.action_fingerprint_onboarding_to_confirmation
            Pair(R.id.fragment_fingerprint_onboarding, R.id.fragment_autofill_onboarding) ->
                R.id.action_onboarding_fingerprint_to_autofill

            Pair(R.id.fragment_autofill_onboarding, R.id.fragment_item_list) -> R.id.action_to_itemList

            Pair(R.id.fragment_onboarding_confirmation, R.id.fragment_item_list) -> R.id.action_to_itemList
            Pair(R.id.fragment_onboarding_confirmation, R.id.fragment_webview) -> R.id.action_to_webview

            Pair(R.id.fragment_locked, R.id.fragment_item_list) -> R.id.action_to_itemList
            Pair(R.id.fragment_locked, R.id.fragment_welcome) -> R.id.action_locked_to_welcome

            Pair(R.id.fragment_item_list, R.id.fragment_item_detail) -> R.id.action_itemList_to_itemDetail
            Pair(R.id.fragment_item_list, R.id.fragment_setting) -> R.id.action_itemList_to_setting
            Pair(R.id.fragment_item_list, R.id.fragment_account_setting) -> R.id.action_itemList_to_accountSetting
            Pair(R.id.fragment_item_list, R.id.fragment_locked) -> R.id.action_to_locked
            Pair(R.id.fragment_item_list, R.id.fragment_filter) -> R.id.action_itemList_to_filter
            Pair(R.id.fragment_item_list, R.id.fragment_webview) -> R.id.action_to_webview

            Pair(R.id.fragment_item_detail, R.id.fragment_webview) -> R.id.action_to_webview

            Pair(R.id.fragment_setting, R.id.fragment_webview) -> R.id.action_to_webview

            Pair(R.id.fragment_account_setting, R.id.fragment_welcome) -> R.id.action_to_welcome

            Pair(R.id.fragment_filter, R.id.fragment_item_detail) -> R.id.action_filter_to_itemDetail

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
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.view.DialogFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemDetailFragmentArgs

class RoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,

    private val accountStore: AccountStore = AccountStore.shared
) : Presenter() {
    private lateinit var navController: NavController

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.fragment_nav_host)
        routeStore.routes.subscribe(this::route).addTo(compositeDisposable)

        accountStore.syncCredentials
            .filterNotNull()
            .subscribe {
                dispatcher.dispatch(DataStoreAction.UpdateCredentials(it))
            }
            .addTo(compositeDisposable)
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> navigateToFragment(action, R.id.fragment_welcome)
            is RouteAction.Login -> navigateToFragment(action, R.id.fragment_fxa_login)
            is RouteAction.ItemList -> navigateToFragment(action, R.id.fragment_item_list)
            is RouteAction.SettingList -> navigateToFragment(action, R.id.fragment_setting)
            is RouteAction.AccountSetting -> navigateToFragment(action, R.id.fragment_account_setting)
            is RouteAction.LockScreen -> navigateToFragment(action, R.id.fragment_locked)
            is RouteAction.Filter -> navigateToFragment(action, R.id.fragment_filter)
            is RouteAction.ItemDetail -> {
                // Possibly overkill for passing a single id string,
                // but it's typesafe™.
                val bundle = ItemDetailFragmentArgs.Builder()
                    .setItemId(action.id)
                    .build()
                    .toBundle()
                navigateToFragment(action, R.id.fragment_item_detail, bundle)
            }
            is RouteAction.OpenWebsite -> {
                openWebsite(action.url)
            }
            is RouteAction.SystemSetting -> {
                openSetting(action)
            }

            is RouteAction.DialogFragment -> showDialogFragment(FingerprintAuthDialogFragment(), action)
            is RouteAction.Dialog -> showDialog(action)

            is RouteAction.Back -> navController.popBackStack()
        }
    }

    private fun showDialog(destination: RouteAction.Dialog) {
        val dialogStateObservable = when (destination) {
            is RouteAction.Dialog.SecurityDisclaimer -> showSecurityDisclaimerDialog()
            is RouteAction.Dialog.UnlinkDisclaimer -> showUnlinkDisclaimerDialog()
        }

        dialogStateObservable
            .map { alertState ->
                val action: Action? = when (alertState) {
                    AlertState.BUTTON_POSITIVE -> {
                        destination.positiveButtonActions
                    }
                    AlertState.BUTTON_NEGATIVE -> {
                        destination.negativeButtonActions
                    }
                }

                action.asOptional()
            }
            .filterNotNull()
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun showDialogFragment(dialogFragment: DialogFragment?, destination: RouteAction.DialogFragment) {
        if (dialogFragment != null) {
            val fragmentManager = activity.supportFragmentManager
            try {
                dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
                dialogFragment.setupDialog(destination.dialogTitle, destination.dialogSubtitle)
            } catch (e: IllegalStateException) {
                log.error("Could not show dialog", e)
            }
        }
    }

    private fun showSecurityDisclaimerDialog(): Observable<AlertState> {
        return AlertDialogHelper.showAlertDialog(
            activity,
            R.string.no_device_security_title,
            R.string.no_device_security_message,
            R.string.set_up_security_button,
            R.string.cancel
        )
    }

    private fun showUnlinkDisclaimerDialog(): Observable<AlertState> {
        return AlertDialogHelper.showAlertDialog(
            activity,
            R.string.disconnect_disclaimer_title,
            R.string.disconnect_disclaimer_message,
            R.string.disconnect,
            R.string.cancel,
            R.color.red
        )
    }

    private fun navigateToFragment(action: RouteAction, @IdRes destinationId: Int, args: Bundle? = null) {
        val src = navController.currentDestination ?: return
        val srcId = src.id
        if (srcId == destinationId && args == null) {
            // No point in navigating if nothing has changed.
            return
        }

        val transition = findTransitionId(srcId, destinationId) ?: destinationId

        if (transition == destinationId) {
            // Without being able to detect if we're in developer mode,
            // it is too dangerous to RuntimeException.
            log.error(
                "Cannot route from ${src.label} to $action. " +
                    "This is a developer bug, fixable by adding an action to graph_main.xml"
            )
        }
        navController.navigate(transition, args)
    }

    private fun findTransitionId(@IdRes from: Int, @IdRes to: Int): Int? {
        // This maps two nodes in the graph_main.xml to the edge between them.
        // If a RouteAction is called from a place the graph doesn't know about then
        // the app will log.error.
        when (Pair(from, to)) {
            Pair(R.id.fragment_welcome, R.id.fragment_fxa_login) -> return R.id.action_welcome_to_fxaLogin

            Pair(R.id.fragment_fxa_login, R.id.fragment_item_list) -> return R.id.action_fxaLogin_to_itemList
            Pair(R.id.fragment_locked, R.id.fragment_item_list) -> return R.id.action_locked_to_itemList

            Pair(R.id.fragment_item_list, R.id.fragment_item_detail) -> return R.id.action_itemList_to_itemDetail
            Pair(R.id.fragment_item_list, R.id.fragment_setting) -> return R.id.action_itemList_to_setting
            Pair(R.id.fragment_item_list, R.id.fragment_account_setting) -> return R.id.action_itemList_to_accountSetting
            Pair(R.id.fragment_item_list, R.id.fragment_locked) -> return R.id.action_itemList_to_locked
            Pair(R.id.fragment_item_list, R.id.fragment_filter) -> return R.id.action_itemList_to_filter

            Pair(R.id.fragment_filter, R.id.fragment_item_detail) -> return R.id.action_filter_to_itemDetail
        }

        return null
    }

    private fun openWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(browserIntent, null)
    }

    private fun openSetting(settingAction: RouteAction.SystemSetting) {
        val settingIntent = Intent(settingAction.setting.intentAction)
        activity.startActivity(settingIntent, null)
    }
}

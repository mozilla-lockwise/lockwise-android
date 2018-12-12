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
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.DataStore.State
import mozilla.lockbox.store.LifecycleStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.FixedDataStoreSupport
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.view.DialogFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemDetailFragmentArgs

@ExperimentalCoroutinesApi
class RoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val accountStore: AccountStore = AccountStore.shared,
    private val lifecycleStore: LifecycleStore = LifecycleStore.shared
) : Presenter() {
    private lateinit var navController: NavController

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.fragment_nav_host)

        // Make the routing contingent on the state of the dataStore.
        val dataStoreRoutes = dataStore.state
            .map(this::dataStoreToRouteActions)
            .filterNotNull()

        // Moves credentials from the AccountStore, into the DataStore.
        Observables.combineLatest(accountStore.syncCredentials, dataStore.state)
            .map(this::accountToDataStoreActions)
            .filterNotNull()
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        routeStore.routes
            .mergeWith(dataStoreRoutes)
            .observeOn(mainThread())
            .subscribe(this::route)
            .addTo(compositeDisposable)
    }

    private fun dataStoreToRouteActions(storageState: State): Optional<RouteAction> {
        return when (storageState) {
            is State.Unlocked -> RouteAction.ItemList
            is State.Locked -> RouteAction.LockScreen
            is State.Unprepared -> RouteAction.Welcome
            else -> RouteAction.LockScreen
        }.asOptional()
    }

    private fun accountToDataStoreActions(it: Pair<Optional<SyncCredentials>, State>): Optional<DataStoreAction> {
        val (opt, state) = it

        val credentials = opt.value ?: return DataStoreAction.Reset.asOptional()

        if (state != State.Unprepared) {
            return Optional(null)
        }

        if (credentials is FixedSyncCredentials) {
            dataStore.resetSupport(FixedDataStoreSupport.shared)
        }

        return DataStoreAction.UpdateCredentials(credentials).asOptional()
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> navigateToFragment(action, R.id.fragment_item_list)
            is RouteAction.Login -> navigateToFragment(action, R.id.fragment_item_list)
            is RouteAction.ItemList -> navigateToFragment(action, R.id.fragment_item_list)
            is RouteAction.SettingList -> navigateToFragment(action, R.id.fragment_setting)
            is RouteAction.FaqList -> navigateToFragment(action, R.id.fragment_webview)
            is RouteAction.SendFeedback -> navigateToFragment(action, R.id.fragment_webview)
            is RouteAction.AccountSetting -> navigateToFragment(action, R.id.fragment_account_setting)
            is RouteAction.LockScreen -> navigateToFragment(action, R.id.fragment_item_list)
            is RouteAction.Filter -> navigateToFragment(action, R.id.fragment_filter)
            is RouteAction.ItemDetail -> navigateToFragment(action, R.id.fragment_item_detail, bundle(action))
            is RouteAction.OpenWebsite -> openWebsite(action.url)
            is RouteAction.SystemSetting -> openSetting(action)
            is RouteAction.DialogFragment -> showDialogFragment(FingerprintAuthDialogFragment(), action)
            is RouteAction.Dialog -> showDialog(action)
            is RouteAction.AutoLockSetting -> showAutoLockSelections()
        }
    }

    private fun bundle(action: RouteAction.ItemDetail): Bundle {
        // Possibly overkill for passing a single id string,
        // but it's typesafe™.
        return ItemDetailFragmentArgs.Builder()
            .setItemId(action.id)
            .build()
            .toBundle()
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
                        destination.positiveButtonAction
                    }
                    AlertState.BUTTON_NEGATIVE -> {
                        destination.negativeButtonAction
                    }
                }

                action.asOptional()
            }
            .filterNotNull()
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
            val from = activity.resources.getResourceName(srcId)
            val to = activity.resources.getResourceName(destinationId)
            log.error("Cannot route from $from to $to. " +
                    "This is a developer bug, fixable by adding an action to graph_main.xml"
            )
        } else {
            val clearBackStack = src.getAction(transition)?.navOptions?.shouldLaunchSingleTop() ?: false
            if (clearBackStack) {
                while (navController.popBackStack()) {
                    // NOP
                }
            }
        }

        try {
            navController.navigate(transition, args)
        } catch (e: IllegalArgumentException) {
            log.error("This appears to be a bug in navController", e)
            navController.navigate(destinationId, args)
        }
    }

    private fun findTransitionId(@IdRes from: Int, @IdRes to: Int): Int? {
        // This maps two nodes in the graph_main.xml to the edge between them.
        // If a RouteAction is called from a place the graph doesn't know about then
        // the app will log.error.
        return when (Pair(from, to)) {
            Pair(R.id.fragment_welcome, R.id.fragment_item_list) -> R.id.action_to_itemList

            Pair(R.id.fragment_welcome, R.id.fragment_locked) -> R.id.action_to_locked
            Pair(R.id.fragment_welcome, R.id.fragment_item_list) -> R.id.action_to_itemList

            Pair(R.id.fragment_fxa_login, R.id.fragment_item_list) -> R.id.action_fxaLogin_to_itemList
            Pair(R.id.fragment_locked, R.id.fragment_item_list) -> R.id.action_to_itemList

            Pair(R.id.fragment_item_list, R.id.fragment_item_detail) -> R.id.action_itemList_to_itemDetail
            Pair(R.id.fragment_item_list, R.id.fragment_setting) -> R.id.action_itemList_to_setting
            Pair(R.id.fragment_item_list, R.id.fragment_account_setting) -> R.id.action_itemList_to_accountSetting
            Pair(R.id.fragment_item_list, R.id.fragment_locked) -> R.id.action_itemList_to_locked
            Pair(R.id.fragment_item_list, R.id.fragment_filter) -> R.id.action_itemList_to_filter
            Pair(R.id.fragment_item_list, R.id.fragment_faq) -> R.id.action_itemList_to_faq
            Pair(R.id.fragment_item_list, R.id.fragment_send_feedback) -> R.id.action_itemList_to_feedback

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
        activity.startActivity(settingIntent, null)
    }
}

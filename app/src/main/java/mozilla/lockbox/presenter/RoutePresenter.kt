/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.RouteStore

open class RoutePresenter(
    private val activity: AppCompatActivity,
    private val dispatcher: Dispatcher,
    private val routeStore: RouteStore
) : Presenter() {
    private lateinit var navController: NavController



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

            R.id.fragment_filter to R.id.fragment_item_detail -> R.id.action_filter_to_itemDetail

            // autofill routes
            Pair(R.id.fragment_locked, R.id.fragment_filter) -> R.id.action_locked_to_filter
            Pair(R.id.fragment_null, R.id.fragment_filter) -> R.id.action_to_filter
            Pair(R.id.fragment_null, R.id.fragment_locked) -> R.id.action_to_locked

            else -> null
        }
    }
}
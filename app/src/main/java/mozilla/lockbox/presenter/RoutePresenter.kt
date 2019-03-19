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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log

@ExperimentalCoroutinesApi
open class RoutePresenter(
    private val activity: AppCompatActivity
) : Presenter() {

    fun navigateToFragment(navController: NavController, @IdRes destinationId: Int, args: Bundle? = null) {
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
            log.error(
                "Cannot route from $from to $to. " +
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

            // autofill transitions
            Pair(R.id.fragment_locked, R.id.fragment_filter) -> R.id.action_locked_to_filter
            Pair(R.id.fragment_null, R.id.fragment_filter) -> R.id.action_to_filter
            Pair(R.id.fragment_null, R.id.fragment_locked) -> R.id.action_to_locked

            else -> null
        }
    }
}
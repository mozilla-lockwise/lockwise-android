/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.ItemDetailFragmentArgs

class RoutePresenter(
    private val activity: AppCompatActivity,
    private val routeStore: RouteStore = RouteStore.shared
) : Presenter() {
    private lateinit var navController: NavController

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.fragment_nav_host)
        routeStore.routes.subscribe(this::route).addTo(compositeDisposable)
    }

    private fun replaceFragment(@IdRes destinationId: Int, args: Bundle? = null) {
        val from = navController.currentDestination?.id ?: return
        if (from == destinationId && args == null) {
            // No point in navigating if nothing has changed.
            return
        }
        val transition = findTransitionId(from, destinationId) ?: destinationId
        navController.navigate(transition, args)
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> replaceFragment(R.id.fragment_welcome)
            is RouteAction.Login -> replaceFragment(R.id.fragment_fxa_login)
            is RouteAction.ItemList -> replaceFragment(R.id.fragment_item_list)
            is RouteAction.SettingList -> replaceFragment(R.id.fragment_setting)
            is RouteAction.LockScreen -> replaceFragment(R.id.fragment_locked)
            is RouteAction.Filter -> replaceFragment(R.id.fragment_filter)
            is RouteAction.ItemDetail -> {
                // Possibly overkill for passing a single id string,
                // but it's typesafe™.
                val bundle = ItemDetailFragmentArgs.Builder()
                        .setItemId(action.id)
                        .build()
                        .toBundle()
                replaceFragment(R.id.fragment_item_detail, bundle)
            }
            is RouteAction.Back -> navController.popBackStack()
        }
    }

    private fun findTransitionId(@IdRes from: Int, @IdRes to: Int): Int? {
        when (from) {
            R.id.fragment_welcome ->
                when (to) {
                    R.id.fragment_fxa_login -> return R.id.action_welcome_to_fxaLogin
                }
            R.id.fragment_fxa_login ->
                when (to) {
                    R.id.fragment_item_list -> return R.id.action_fxaLogin_to_itemList
                }
            R.id.fragment_item_list ->
                when (to) {
                    R.id.fragment_item_detail -> return R.id.action_itemList_to_itemDetail
                    R.id.fragment_setting -> return R.id.action_itemList_to_setting
                    R.id.fragment_locked -> return R.id.action_itemList_to_locked
                    R.id.fragment_filter -> return R.id.action_itemList_to_filter
                }
            R.id.fragment_filter ->
                when (to) {
                    R.id.fragment_item_detail -> return R.id.action_filter_to_itemDetail
                }
        }

        log.warn("Cannot find a transition between $from and $to")
        return null
    }
}

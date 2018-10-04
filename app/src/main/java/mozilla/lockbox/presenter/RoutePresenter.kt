/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

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

class RoutePresenter(
        private val activity: AppCompatActivity,
        private val routeStore: RouteStore = RouteStore.shared
) : Presenter() {
    private lateinit var navController: NavController

    override fun onViewReady() {
        navController = Navigation.findNavController(activity, R.id.fragment_nav_host)
        routeStore.routes.subscribe(this::route).addTo(compositeDisposable)
    }

    private fun replaceFragment(@IdRes destinationId: Int) {
        val from = navController.currentDestination?.id ?: return
        if (from == destinationId) {
            return
        }
        val transition = findTransitionId(from, destinationId) ?: destinationId
        navController.navigate(transition)
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> replaceFragment(R.id.welcomeFragment)
            is RouteAction.Login -> replaceFragment(R.id.fxaLoginFragment)
            is RouteAction.ItemList -> replaceFragment(R.id.itemListFragment)
            is RouteAction.SettingList -> replaceFragment(R.id.settingFragment)
            is RouteAction.LockScreen -> replaceFragment(R.id.lockedFragment)
            is RouteAction.Filter -> replaceFragment(R.id.filterFragment)
            is RouteAction.ItemDetail -> {
//                itemDetail.itemId = action.id
                replaceFragment(R.id.itemDetailFragment)
            }
            is RouteAction.Back -> navController.popBackStack()
        }
    }

    private fun findTransitionId(@IdRes from: Int, @IdRes to: Int) : Int? {
        when (from) {
            R.id.welcomeFragment ->
                when (to) {
                    R.id.fxaLoginFragment -> return R.id.action_welcomeFragment_to_fxaLoginFragment
                }
            R.id.fxaLoginFragment ->
                when (to) {
                    R.id.itemListFragment -> return R.id.action_fxaLoginFragment_to_itemListFragment
                }
            R.id.itemListFragment ->
                when (to) {
                    R.id.itemDetailFragment -> return R.id.action_itemListFragment_to_itemDetailFragment
                    R.id.settingFragment -> return R.id.action_itemListFragment_to_settingFragment
                    R.id.lockedFragment -> return R.id.action_itemListFragment_to_lockedFragment
                    R.id.filterFragment -> return R.id.action_itemListFragment_to_filterFragment
                }
            R.id.filterFragment ->
                when (to) {
                    R.id.itemDetailFragment -> return R.id.action_filterFragment_to_itemDetailFragment
                }
        }

        log.warn("Cannot find a transition between ${from} and ${to}")
        return null
    }
}

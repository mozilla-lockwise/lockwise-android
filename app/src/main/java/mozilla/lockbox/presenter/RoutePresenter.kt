/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.FxALoginFragment
import mozilla.lockbox.view.ItemListFragment
import mozilla.lockbox.view.SettingFragment
import mozilla.lockbox.view.WelcomeFragment
import mozilla.lockbox.view.LockedFragment
import mozilla.lockbox.view.ItemDetailFragment
import mozilla.lockbox.view.FilterFragment

class RoutePresenter(private val activity: AppCompatActivity, routeStore: RouteStore = RouteStore.shared) : Presenter() {
    private val welcome: WelcomeFragment by lazy { WelcomeFragment() }
    private val login: FxALoginFragment by lazy { FxALoginFragment() }
    private val itemList: ItemListFragment by lazy { ItemListFragment() }
    private val settingList: SettingFragment by lazy { SettingFragment() }
    private val lock: LockedFragment by lazy { LockedFragment() }
    private val itemDetail: ItemDetailFragment by lazy { ItemDetailFragment() }
    private val filter: FilterFragment by lazy { FilterFragment() }

    init {
        routeStore.routes
                .subscribe(this::route)
                .addTo(compositeDisposable)
    }

    override fun onViewReady() {
        replaceFragment(this.welcome, false)
    }

    private fun replaceFragment(frag: Fragment, backable: Boolean = true) {
        val tx = activity.supportFragmentManager.beginTransaction()
        tx.replace(R.id.root_content, frag)
        if (backable) {
            tx.addToBackStack(null)
        }
        tx.commit()

        if (!backable) {
            clearBackStack()
        }
    }

    private fun clearBackStack() {
        val fm = activity.supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            val base = fm.getBackStackEntryAt(0)
            fm.popBackStackImmediate(base.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun route(action: RouteAction) {
        when (action) {
            is RouteAction.Welcome -> replaceFragment(welcome, false)
            is RouteAction.Login -> replaceFragment(login)
            is RouteAction.ItemList -> replaceFragment(itemList, false)
            is RouteAction.SettingList -> replaceFragment(settingList)
            is RouteAction.LockScreen -> replaceFragment(lock, false)
            is RouteAction.Filter -> replaceFragment(filter)
            is RouteAction.ItemDetail -> {
                itemDetail.itemId = action.id
                replaceFragment(itemDetail)
            }
            is RouteAction.Back -> activity.supportFragmentManager.popBackStack()
        }
    }
}

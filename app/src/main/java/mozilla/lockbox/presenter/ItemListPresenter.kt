/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.view.MenuItem
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.titleFromHostname
import mozilla.lockbox.flux.Dispatcher

import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

interface ItemListView {
    val drawerItemSelections: Observable<MenuItem>
    fun updateItems(itemList: List<ItemViewModel>)
    fun closeDrawers()
    // TODO: Item list selection
}

class ItemListPresenter(private val view: ItemListView,
                        private val dispatcher: Dispatcher = Dispatcher.shared,
                        private val dataStore: DataStore = DataStore.shared) : Presenter() {
    override fun onViewReady() {
        view.drawerItemSelections
            .subscribe { menuItem ->
                view.closeDrawers()
                when (menuItem.itemId) {
                    R.id.goto_settings -> {
                        dispatcher.dispatch(RouteAction.SETTING_LIST)
                    }
                    R.id.lock_now -> {
                        dispatcher.dispatch(RouteAction.LOCK)
                    }
                    else -> {
                        log.info("Menu ${menuItem.title} unimplemented")
                    }
                }
            }
            .addTo(compositeDisposable)

        dataStore.list
            .map {
                it.map {
                    val username = it.username ?: ""
                    val hostname = it.hostname.titleFromHostname()
                    ItemViewModel(
                            hostname,
                            username,
                            it.id)
                }
            }
            .subscribe(view::updateItems)
            .addTo(compositeDisposable)

        dispatcher.dispatch(DataStoreAction.Type.UNLOCK)
    }
}
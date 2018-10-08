/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.view.MenuItem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Dispatcher

import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

interface ItemListView {
    val drawerItemSelections: Observable<MenuItem>
    val itemSelection: Observable<ItemViewModel>
    val filterClicks: Observable<Unit>
    fun updateItems(itemList: List<ItemViewModel>)
    fun closeDrawers()
    // TODO: Item list selection
}

class ItemListPresenter(
    private val view: ItemListView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared
) : Presenter() {
    override fun onViewReady() {
        view.drawerItemSelections
                .subscribe { menuItem ->
                    view.closeDrawers()
                    when (menuItem.itemId) {
                        R.id.goto_settings -> {
                            dispatcher.dispatch(RouteAction.SettingList)
                        }
                        R.id.lock_now -> {
                            dispatcher.dispatch(RouteAction.LockScreen)
                        }
                        else -> {
                            log.info("Menu ${menuItem.title} unimplemented")
                        }
                    }
                }
                .addTo(compositeDisposable)

        dataStore.list
                .filter { it.isNotEmpty() }
                .mapToItemViewModelList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::updateItems)
                .addTo(compositeDisposable)

        view.itemSelection
                .subscribe { it ->
                    dispatcher.dispatch(RouteAction.ItemDetail(it.guid))
                }
                .addTo(compositeDisposable)

        view.filterClicks
                .subscribe {
                    dispatcher.dispatch(RouteAction.Filter)
                }
                .addTo(compositeDisposable)

        // TODO: remove this when we have proper locking / unlocking
        dispatcher.dispatch(DataStoreAction.Unlock)
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.IdRes
import android.view.MenuItem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SortAction
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.extensions.sort
import mozilla.lockbox.flux.Dispatcher

import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.PublicPreferencesStore

interface ItemListView {
    val itemSelection: Observable<ItemViewModel>
    val filterClicks: Observable<Unit>
    val menuItemSelections: Observable<Int>
    val sortItemSelection: Observable<MenuItem>
    fun updateItems(itemList: List<ItemViewModel>)
    fun updateItemListSort(itemId: Int)
    // TODO: Item list selection
}

class ItemListPresenter(
    private val view: ItemListView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val prefsStore: PublicPreferencesStore = PublicPreferencesStore.shared
) : Presenter() {

    override fun onViewReady() {
        val itemViewModelList = dataStore.list
                .filter { it.isNotEmpty() }
                .mapToItemViewModelList()

        Observables.combineLatest(itemViewModelList, prefsStore.itemListSortObservable)
                .distinctUntilChanged()
                .sort()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::updateItems)
                .addTo(compositeDisposable)

        prefsStore.itemListSortObservable
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::updateItemListSort)
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

        view.menuItemSelections
            .subscribe(this::onMenuItem)
            .addTo(compositeDisposable)

        view.sortItemSelection
                .subscribe { menuItem ->
                    when (menuItem.itemId) {
                        R.id.sort_a_z -> {
                            dispatcher.dispatch(SortAction.Alphabetically)
                        }
                        R.id.sort_recent -> {
                            dispatcher.dispatch(SortAction.RecentlyUsed)
                        }
                        else -> {
                            log.info("Menu ${menuItem.title} unimplemented")
                        }
                    }
                }.addTo(compositeDisposable)

        // TODO: remove this when we have proper locking / unlocking
        dispatcher.dispatch(DataStoreAction.Unlock)
    }

    private fun onMenuItem(@IdRes item: Int) {
        val action = when (item) {
            R.id.fragment_locked -> RouteAction.LockScreen
            R.id.fragment_setting -> RouteAction.SettingList
            else -> return log.error("Cannot route from item list menu")
        }

        dispatcher.dispatch(action)
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.IdRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.ItemListSort
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingsAction
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Dispatcher

import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.PreferencesStore

interface ItemListView {
    val itemSelection: Observable<ItemViewModel>
    val filterClicks: Observable<Unit>
    val menuItemSelections: Observable<Int>
    val sortItemSelection: Observable<ItemListSort>
    fun updateItems(itemList: List<ItemViewModel>)
    fun updateItemListSort(sort: ItemListSort)
}

class ItemListPresenter(
    private val view: ItemListView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val prefsStore: PreferencesStore = PreferencesStore.shared
) : Presenter() {

    override fun onViewReady() {
        Observables.combineLatest(dataStore.list, prefsStore.itemListSortObservable)
                .filter { it.first.isNotEmpty() }
                .distinctUntilChanged()
                .map { pair ->
                    when (pair.second) {
                        ItemListSort.ALPHABETICALLY -> { pair.first.sortedBy { titleFromHostname(it.hostname) } }
                        ItemListSort.RECENTLY_USED -> { pair.first.sortedBy { -it.timeLastUsed } }
                    }
                }
                .mapToItemViewModelList()
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
                .subscribe { sortBy ->
                    dispatcher.dispatch(SettingsAction.SortAction(sortBy))
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
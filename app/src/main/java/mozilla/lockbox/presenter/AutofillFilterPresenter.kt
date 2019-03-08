/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

interface AutofillFilterView {
    val onDismiss: Observable<Unit>
    val filterTextEntered: Observable<CharSequence>
    val filterText: Consumer<in CharSequence>
    val cancelButtonClicks: Observable<Unit>
    val cancelButtonVisibility: Consumer<in Boolean>
    val itemSelection: Observable<ItemViewModel>
    fun updateItems(items: List<ItemViewModel>)
    fun displayNoEntries(enabled: Boolean)
}

@ExperimentalCoroutinesApi
class AutofillFilterPresenter(
    val view: AutofillFilterView,
    val dispatcher: Dispatcher = Dispatcher.shared,
    val dataStore: DataStore = DataStore.shared
) : Presenter() {
    override fun onViewReady() {
        view.onDismiss
            .map { AutofillAction.Cancel }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        val itemViewModelList = dataStore.list.mapToItemViewModelList()

        val filteredItems = Observables.combineLatest(view.filterTextEntered, itemViewModelList)
            .map { pair ->
                Pair(pair.first, pair.second.filter {
                    it.title.contains(pair.first, true) ||
                        it.subtitle.contains(pair.first, true)
                })
            }

        filteredItems
            .map { if (it.first.isEmpty()) emptyList() else it.second }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::updateItems)
            .addTo(compositeDisposable)

        filteredItems
            .map { !it.first.isEmpty() || it.second.isEmpty() }
            .subscribe(view::displayNoEntries)
            .addTo(compositeDisposable)

        view.filterTextEntered
            .map { it.isNotEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view.cancelButtonVisibility)
            .addTo(compositeDisposable)

        view.cancelButtonClicks
            .map { "" }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view.filterText)
            .addTo(compositeDisposable)

        view.itemSelection
            .switchMap { dataStore.get(it.guid) }
            .map {
                it.value?.let { serverPassword ->
                    AutofillAction.Complete(serverPassword)
                } ?: AutofillAction.Cancel
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }
}
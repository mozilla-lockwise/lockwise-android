/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.CallSuper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

interface FilterView {
    val onDismiss: Observable<Unit>?
    val filterTextEntered: Observable<CharSequence>
    val filterText: Consumer<in CharSequence>
    val cancelButtonClicks: Observable<Unit>
    val cancelButtonVisibility: Consumer<in Boolean>
    val itemSelection: Observable<ItemViewModel>
    val displayNoEntries: ((Boolean) -> Unit)?
    fun updateItems(items: List<ItemViewModel>)
}

@ExperimentalCoroutinesApi
abstract class FilterPresenter(
    open val view: FilterView,
    open val dispatcher: Dispatcher,
    open val dataStore: DataStore
) : Presenter() {

    protected abstract fun Observable<ItemViewModel>.itemSelectionActionMap(): Observable<Action>

    protected abstract fun Observable<Pair<CharSequence, List<ItemViewModel>>>.itemListMap(): Observable<List<ItemViewModel>>

    @CallSuper
    override fun onViewReady() {
        val itemViewModelList = dataStore.list.mapToItemViewModelList()

        val filteredItems = Observables.combineLatest(view.filterTextEntered, itemViewModelList)
            .map { pair ->
                Pair(pair.first, pair.second.filter {
                    it.title.contains(pair.first, true) ||
                        it.subtitle.contains(pair.first, true)
                })
            }

        filteredItems
            .itemListMap()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::updateItems)
            .addTo(compositeDisposable)

        view.displayNoEntries?.let { noEntriesConsumer ->
            filteredItems
                .map { !it.first.isEmpty() || it.second.isEmpty() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(noEntriesConsumer)
                .addTo(compositeDisposable)
        }

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
            .itemSelectionActionMap()
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.onDismiss
            ?.map { AutofillAction.Cancel }
            ?.subscribe(dispatcher::dispatch)
            ?.addTo(compositeDisposable)
    }
}
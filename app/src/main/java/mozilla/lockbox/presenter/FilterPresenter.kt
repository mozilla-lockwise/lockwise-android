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
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

interface FilterView {
    val filterTextEntered: Observable<CharSequence>
    val filterText: Consumer<in CharSequence>
    val cancelButtonClicks: Observable<Unit>
    val cancelButtonVisibility: Consumer<in Boolean>
    fun updateItems(items: List<ItemViewModel>)
}

class FilterPresenter(
        val view: FilterView,
        val dataStore: DataStore = DataStore.shared
        ) : Presenter() {

    override fun onViewReady() {
        val itemViewModelList = dataStore.list.mapToItemViewModelList()

        Observables.combineLatest(view.filterTextEntered, itemViewModelList)
                .filterItemsForText()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::updateItems)
                .addTo(compositeDisposable)
    }

    private fun Observable<Pair<CharSequence, List<ItemViewModel>>>.filterItemsForText(): Observable<List<ItemViewModel>> {
        return this.map { pair ->
            log.debug("mapping")
            pair.second.filter {
                it.title.contains(pair.first, true) ||
                        it.subtitle.contains(pair.first, true)
            }
        }

    }
}

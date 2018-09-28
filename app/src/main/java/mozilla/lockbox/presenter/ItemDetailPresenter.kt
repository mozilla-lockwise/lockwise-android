/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

interface ItemDetailView {
    var itemId: String?
    fun updateItem(item: ItemViewModel)
}

class ItemDetailPresenter(
        private val view: ItemDetailView,
        private val dispatcher: Dispatcher = Dispatcher.shared,
        private val dataStore: DataStore = DataStore.shared
) : Presenter() {
    override fun onViewReady() {
        val itemId = view?.itemId ?: return
        dataStore.get(itemId).map { ItemViewModel(it.hostname, it.username?: "", it.id) }
                .subscribe(view::updateItem)
                .addTo(compositeDisposable)
    }
}
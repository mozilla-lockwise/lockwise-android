/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.AutofillAction
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
}

@ExperimentalCoroutinesApi
class AutofillFilterPresenter(
    val view: AutofillFilterView,
    val dispatcher: Dispatcher = Dispatcher.shared,
    val dataStore: DataStore = DataStore.shared
) : Presenter() {
    override fun onViewReady() {
        view.onDismiss
            .map {
                AutofillAction.Cancel
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }
}
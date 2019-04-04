/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore

@ExperimentalCoroutinesApi
open class AutofillFilterPresenter(
    override val view: FilterView,
    override val dispatcher: Dispatcher = Dispatcher.shared,
    override val dataStore: DataStore = DataStore.shared
) : FilterPresenter(view, dispatcher, dataStore) {

    override fun Observable<ItemViewModel>.itemSelectionActionMap(): Observable<Action> {
        return this.switchMap { dataStore.get(it.id) }
            .map {
                it.value?.let { serverPassword ->
                    AutofillAction.Complete(serverPassword)
                } ?: AutofillAction.Cancel
            }
    }

    override fun Observable<Pair<CharSequence, List<ItemViewModel>>>.itemListMap(): Observable<List<ItemViewModel>> {
        return this.map { if (it.first.isEmpty()) emptyList() else it.second }
    }
}
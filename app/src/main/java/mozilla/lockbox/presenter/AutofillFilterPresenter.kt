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
import mozilla.lockbox.model.ItemViewModel

@ExperimentalCoroutinesApi
class AutofillFilterPresenter(
    override val view: FilterView
) : FilterPresenter(view) {

    override fun itemSelectionAction(id: String): Observable<Action> {
        return dataStore.get(id)
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
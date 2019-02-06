/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.support

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.AutoLockStore

class AccountStoreSupport {
    companion object {
        fun prepareDataStoreActions(
            accountStore: AccountStore,
            autoLockStore: AutoLockStore? = null,
            dispatcher: Dispatcher,
            compositeDisposable: CompositeDisposable
        ) {
            val autoLockActions = if (autoLockStore != null) {
                autoLockStore.autoLockActivated
                    .filter { it }
                    .map { DataStoreAction.Lock }
            } else {
                Observable.empty()
            }

            // Moves credentials from the AccountStore, into the DataStore.
            accountStore.syncCredentials
                .map(this::accountToDataStoreActions)
                .mergeWith(autoLockActions)
                .subscribe(dispatcher::dispatch)
                .addTo(compositeDisposable)
        }

        private fun accountToDataStoreActions(optCredentials: Optional<SyncCredentials>): DataStoreAction {
            // we will get a null credentials object (and subsequently reset the datastore) on
            // both initial login and reset / logout.
            val credentials = optCredentials.value ?: return DataStoreAction.Reset

            return DataStoreAction.UpdateCredentials(credentials)
        }
    }

}
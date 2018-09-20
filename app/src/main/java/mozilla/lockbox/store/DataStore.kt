/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.SingleSubject
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncResult

data class DataStoreState(
  val status: Status,
  val error: Throwable? = null
) {
    enum class Status {
        UNPREPARED,
        LOCKED,
        UNLOCKED,
        ERRORED
    }
}

/**
 *
 */
class DataStore(
        val dispatcher: Dispatcher = Dispatcher.shared,
        val support: DataStoreSupport = FixedDataStoreSupport.shared
) {
    internal val compositeDisposable = CompositeDisposable()
    private val stateSubject: ReplaySubject<DataStoreState> = ReplaySubject.create(1)
    private val listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())

    private val backend: LoginsStorage

    init {
        // TODO: Replace test data with real backend
        backend = support.createLoginsStorage()

        // handle state changes
        state.subscribe { state ->
            when (state.status) {
                DataStoreState.Status.LOCKED -> clearList()
                DataStoreState.Status.UNLOCKED -> updateList()
                else -> Unit
            }
        }.addTo(compositeDisposable)
    }

    val state: Observable<DataStoreState> get() = stateSubject

    val list: Observable<List<ServerPassword>> get() = listSubject

    fun unlock(): Observable<Unit> {
        val unlockSubject = SingleSubject.create<Unit>()

        backend.isLocked().then {
            if (it) {
                backend.unlock(support.encryptionKey).then {
                    stateSubject.onNext(DataStoreState(DataStoreState.Status.UNLOCKED))
                    SyncResult.fromValue(Unit)
                }
            } else {
                unlockSubject.onSuccess(Unit)
                SyncResult.fromValue(Unit)
            }
        }.then {
            unlockSubject.onSuccess(Unit)
            SyncResult.fromValue(Unit)
        }.thenCatch {
            unlockSubject.onError(it)
            SyncResult.fromValue(Unit)
        }

        return unlockSubject.toObservable()
    }
    fun lock(): Observable<Unit> {
        val lockSubject = SingleSubject.create<Unit>()

        backend.isLocked().then {
            if (!it) {
                backend.lock().then {
                    stateSubject.onNext(DataStoreState(DataStoreState.Status.LOCKED))
                    SyncResult.fromValue(Unit)
                }
            } else {
                SyncResult.fromValue(Unit)
            }
        }.then {
            lockSubject.onSuccess(Unit)
            SyncResult.fromValue(Unit)
        }.thenCatch {
            lockSubject.onError(it)
            SyncResult.fromValue(Unit)
        }

        return lockSubject.toObservable()
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }
    private fun updateList() {
        backend.list().whenComplete { all -> this.listSubject.accept(all) }
    }
}

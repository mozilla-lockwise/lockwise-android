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
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.MemoryLoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncResult

open class DataStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    val support: DataStoreSupport = FixedDataStoreSupport.shared
) {
    companion object {
        val shared = DataStore()
    }

    sealed class State {
        object Unprepared : State()
        object Locked : State()
        object Unlocking: State()
        object Unlocked : State()
        data class Errored(val error: Throwable) : State()
    }

    internal val compositeDisposable = CompositeDisposable()
    private val stateSubject: ReplaySubject<State> = ReplaySubject.create(1)
    private val listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())

    private val backend: LoginsStorage

    init {
        // TODO: Replace test data with real backend
        backend = support.createLoginsStorage()

        // handle state changes
        state.subscribe { state ->
            when (state) {
                is State.Locked -> clearList()
                is State.Unlocked -> updateList()
                else -> Unit
            }
        }.addTo(compositeDisposable)

        // register for actions
        dispatcher.register
                .filterByType(DataStoreAction::class.java)
                .subscribe {
                    when (it) {
                        is DataStoreAction.Lock -> lock()
                        is DataStoreAction.Unlock -> unlock()
                        is DataStoreAction.Sync -> sync()
                        is DataStoreAction.Reset -> reset()
                    }
                }
                .addTo(compositeDisposable)
    }

    val state: Observable<State> get() = stateSubject

    open val list: Observable<List<ServerPassword>> get() = listSubject

    open fun get(id: String): Observable<ServerPassword?> {
        return list.map { items ->
            items.findLast { item -> item.id == id }
        }
    }

    fun unlock(): Observable<Unit> {
        val unlockSubject = SingleSubject.create<Unit>()
        stateSubject.onNext(State.Unlocking)
        backend.isLocked().then {
            if (it) {
                backend.unlock(support.encryptionKey).then {
                    stateSubject.onNext(State.Unlocked)
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
                    stateSubject.onNext(State.Locked)
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

    fun sync(): Observable<Unit> {
        val syncSubject = SingleSubject.create<Unit>()

        backend.sync(support.syncConfig).then {
            updateList()
        }.then {
            syncSubject.onSuccess(Unit)
            SyncResult.fromValue(Unit)
        }.thenCatch {
            stateSubject.onNext(State.Errored(it))
            syncSubject.onError(it)
            SyncResult.fromValue(Unit)
        }

        return syncSubject.toObservable()
    }

    // item list managementa
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }

    private fun updateList(): SyncResult<Unit> {
        return backend.list().then {
            this.listSubject.accept(it)
            SyncResult.fromValue(Unit)
        }
    }

    private fun reset() {
        clearList()
        backend.reset().then({
            this.stateSubject.onNext(DataStore.State.Unprepared)
            SyncResult.fromValue(Unit)
        }, {
            if (backend is MemoryLoginsStorage) {
                updateList()
            }

            this.stateSubject.onNext(DataStore.State.Unprepared)
            SyncResult.fromValue(Unit)
        })
    }
}

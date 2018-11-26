/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.SingleSubject
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncResult
import org.mozilla.sync15.logins.SyncUnlockInfo

open class DataStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    var support: DataStoreSupport = FixedDataStoreSupport.shared
) {
    companion object {
        val shared = DataStore()
    }

    sealed class State {
        object Unprepared : State()
        object Locked : State()
        object Unlocked : State()
        data class Errored(val error: Throwable) : State()
    }

    internal val compositeDisposable = CompositeDisposable()
    private val stateSubject: BehaviorRelay<State> = BehaviorRelay.createDefault(State.Unprepared)
    private val listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())

    private var backend: LoginsStorage

    init {
        backend = support.createLoginsStorage()

        // handle state changes
        state.subscribe { state ->
            when (state) {
                is State.Locked -> clearList()
                is State.Unlocked -> updateList()
                else -> Unit
            }
        }.addTo(compositeDisposable)

        val resetObservable = dispatcher.register
            .filter { it == LifecycleAction.UserReset }
            .map { DataStoreAction.Reset }

        // register for actions
        dispatcher.register
            .mergeWith(resetObservable)
            .filterByType(DataStoreAction::class.java)
            .subscribe { action ->
                when (action) {
                    is DataStoreAction.Lock -> lock()
                    is DataStoreAction.Unlock -> unlock()
                    is DataStoreAction.Sync -> sync()
                    is DataStoreAction.Touch -> touch(action.id)
                    is DataStoreAction.Reset -> reset()
                    is DataStoreAction.UpdateCredentials -> updateCredentials(action.syncCredentials)
                }
            }
            .addTo(compositeDisposable)
    }

    // Warning: this is testing code.
    // It's only called immediately after the user has pressed "Use Test Data".
    fun resetSupport(support: DataStoreSupport) {
        if (stateSubject.value != State.Unprepared) {
            backend.reset()
        }
        this.support = support
        this.backend = support.createLoginsStorage()
        // we shouldn't set the status of this to Unprepared,
        // as we don't want to change any UI.
    }

    private fun touch(id: String) {
        backend.isLocked().whenComplete {
            if (!it) {
                backend.touch(id).then {
                    updateList()
                }
            }
        }
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

        backend.isLocked().then {
            if (it) {
                backend.unlock(support.encryptionKey).then {
                    stateSubject.accept(State.Unlocked)
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
            log.error("Error unlocking", it)
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
                    stateSubject.accept(State.Locked)
                    SyncResult.fromValue(Unit)
                }
            } else {
                SyncResult.fromValue(Unit)
            }
        }.then {
            lockSubject.onSuccess(Unit)
            SyncResult.fromValue(Unit)
        }.thenCatch {
            log.error("Error locking", it)
            lockSubject.onError(it)
            SyncResult.fromValue(Unit)
        }

        return lockSubject.toObservable()
    }

    fun sync(): Observable<Unit> {
        val syncSubject = SingleSubject.create<Unit>()

        val syncConfig = support.syncConfig ?: return {
            val throwable = IllegalStateException("syncConfig should already be defined")
            syncSubject.onError(throwable)
            stateSubject.accept(State.Errored(throwable))
            syncSubject.toObservable()
        }()
        backend.sync(syncConfig).then {
            updateList()
        }.then {
            syncSubject.onSuccess(Unit)
            SyncResult.fromValue(Unit)
        }.thenCatch {
            log.error("Error syncing", it)
            stateSubject.accept(State.Errored(it))
            syncSubject.onError(it)
            SyncResult.fromValue(Unit)
        }

        return syncSubject.toObservable()
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }

    private fun updateList(): SyncResult<Unit> {
        return backend.list().then {
            this.listSubject.accept(it)
            SyncResult.fromValue(Unit)
        }
    }

    fun reset() {
        clearList()

        fun resetState(): SyncResult<Unit> {
            this.stateSubject.accept(State.Unprepared)
            return SyncResult.fromValue(Unit)
        }

        backend.reset()
            .then {
                resetState()
            }
            .thenCatch {
                resetState()
            }
    }

    private fun updateCredentials(credentials: SyncCredentials): Observable<Unit>? {
        if (!credentials.isValid) {
            return null
        }

        credentials.apply {
            support.syncConfig = SyncUnlockInfo(kid, accessToken, syncKey, tokenServerURL)
        }

        return unlock()
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import android.content.SyncResult
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.appservices.logins.LoginsStorage
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport

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

    val state: Observable<State> get() = stateSubject
    open val list: Observable<List<ServerPassword>> get() = listSubject

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
        if (backend.isLocked()) {
            backend.touch(id)
                updateList()
        }
    }

    open fun get(id: String): Observable<ServerPassword?> {
        return list.map { items ->
            items.findLast { item -> item.id == id }
        }
    }

    fun unlock() {
        if (!backend.isLocked()) {
                backend.unlock(support.encryptionKey)
                stateSubject.accept(State.Unlocked)
            }
    }

    private fun lock() {
        if (!backend.isLocked()) {
            backend.lock()
            stateSubject.accept(State.Locked)
        }
    }

    fun sync() {
        val syncConfig = support.syncConfig ?: return {
            val throwable = IllegalStateException("syncConfig should already be defined")
            stateSubject.accept(State.Errored(throwable))
        }()

        backend.sync(syncConfig)
        updateList()
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }

    private fun updateList(){
        this.listSubject.accept(backend.list())
    }

    private fun reset() {
        clearList()
        backend.reset()
        this.stateSubject.accept(State.Unprepared)
    }

    private fun updateCredentials(credentials: SyncCredentials) {
        if (!credentials.isValid) {
            return
        }

        credentials.apply {
            support.syncConfig = SyncUnlockInfo(kid, accessToken, syncKey, tokenServerURL)
        }

        unlock()
    }
}

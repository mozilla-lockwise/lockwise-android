/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asSingle
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.sync.logins.AsyncLoginsStorage
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport

@ExperimentalCoroutinesApi
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

    enum class SyncState {
        Syncing, NotSyncing
    }

    internal val compositeDisposable = CompositeDisposable()
    private val stateSubject = ReplaySubject.createWithSize<State>(1)
    private val listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())

    open val state: Observable<State> = stateSubject
    open val syncState: Observable<SyncState> = ReplaySubject.create()
    open val list: Observable<List<ServerPassword>> get() = listSubject

    private var backend: AsyncLoginsStorage

    init {
        backend = support.createLoginsStorage()
        // handle state changes
        stateSubject
            .subscribe { state ->
                when (state) {
                    is State.Locked -> clearList()
                    is State.Unlocked -> sync()
                    else -> Unit
                }
            }
            .addTo(compositeDisposable)

        // register for actions
        dispatcher.register
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

    open fun get(id: String): Observable<ServerPassword?> {
        return list.map { items ->
            items.findLast { item -> item.id == id }
        }
    }

    private fun touch(id: String) {
        if (!backend.isLocked()) {
            backend.touch(id)
                .asSingle(Dispatchers.Default)
                .subscribe { _, _ ->
                    updateList()
                }
                .addTo(compositeDisposable)
        }
    }

    private fun unlock() {
        if (backend.isLocked()) {
            backend.unlock(support.encryptionKey)
                .asSingle(Dispatchers.Default)
                .subscribe { _, _ ->
                    stateSubject.onNext(State.Unlocked)
                }
                .addTo(compositeDisposable)
        } else {
            stateSubject.onNext(State.Unlocked)
        }
    }

    private fun lock() {
        if (!backend.isLocked()) {
            backend.lock()
                .asSingle(Dispatchers.Default)
                .subscribe { _, _ ->
                    stateSubject.onNext(State.Locked)
                }
                .addTo(compositeDisposable)
        } else {
            stateSubject.onNext(State.Locked)
        }
    }

    private fun sync() {
        val syncConfig = support.syncConfig ?: return {
            val throwable = IllegalStateException("syncConfig should already be defined")
            stateSubject.onNext(State.Errored(throwable))
        }()

        (syncState as Subject).onNext(SyncState.Syncing)

        backend.sync(syncConfig)
            .asSingle(Dispatchers.Default)
            .subscribe { _, _ ->
                updateList()
                (syncState as Subject).onNext(SyncState.NotSyncing)
            }
            .addTo(compositeDisposable)
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }

    private fun updateList() {
        if (!backend.isLocked()) {
            backend.list()
                .asSingle(Dispatchers.Default)
                .subscribe { list, _ ->
                    list?.let {
                        listSubject.accept(it)
                    }
                }
                .addTo(compositeDisposable)
        }
    }

    private fun reset() {
        if (stateSubject.value == State.Unprepared) {
            return
        }
        clearList()
        backend.reset()
            .asSingle(Dispatchers.Default)
            .subscribe { _, _ ->
                this.stateSubject.onNext(State.Unprepared)
            }
            .addTo(compositeDisposable)
    }

    private fun updateCredentials(credentials: SyncCredentials) {
        if (!credentials.isValid) {
            return
        }

        resetSupport(credentials.support)

        credentials.apply {
            support.syncConfig = SyncUnlockInfo(kid, accessToken, syncKey, tokenServerURL)
        }

        if (!credentials.isNew) return

        if (backend.isLocked()) {
            backend.unlock(support.encryptionKey)
                .asSingle(Dispatchers.Default)
                .subscribe { _, _ ->
                    stateSubject.onNext(State.Unlocked)
                }
                .addTo(compositeDisposable)
        } else {
            stateSubject.onNext(State.Unlocked)
        }
    }

    // Warning: this is testing code.
    // It's only called immediately after the user has pressed "Use Test Data".
    fun resetSupport(support: DataStoreSupport) {
        if (support == this.support) {
            return
        }
        if (stateSubject.value != State.Unprepared &&
            stateSubject.value != null) {
            backend.reset()
                .asSingle(Dispatchers.Default)
                .subscribe()
                .addTo(compositeDisposable)
        }
        this.support = support
        this.backend = support.createLoginsStorage()
        // we shouldn't set the status of this to Unprepared,
        // as we don't want to change any UI.
    }
}

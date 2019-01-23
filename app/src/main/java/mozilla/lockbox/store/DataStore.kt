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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asSingle
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.sync.logins.AsyncLoginsStorage
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FixedDataStoreSupport
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import kotlin.coroutines.CoroutineContext

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

    private val exceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, e ->
            log.error(
                message = "Unexpected error occurred during LoginsStorage usage",
                throwable = e
            )
        }

    private val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + exceptionHandler

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

    open fun get(id: String): Observable<Optional<ServerPassword>> {
        return list.map { items ->
            items.findLast { item -> item.id == id }.asOptional()
        }
    }

    private fun touch(id: String) {
        if (!backend.isLocked()) {
            backend.touch(id)
                .asSingle(coroutineContext)
                .subscribe(this::updateList, this::pushError)
                .addTo(compositeDisposable)
        }
    }

    private fun unlock() {
        backend.ensureUnlocked(support.encryptionKey)
            .asSingle(coroutineContext)
            .map { State.Unlocked }
            .subscribe(stateSubject::onNext, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun lock() {
        backend.ensureLocked()
            .asSingle(coroutineContext)
            .map { State.Locked }
            .subscribe(stateSubject::onNext, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun sync() {
        val syncConfig = support.syncConfig ?: return {
            val throwable = IllegalStateException("syncConfig should already be defined")
            stateSubject.onNext(State.Errored(throwable))
        }()

        (syncState as Subject).onNext(SyncState.Syncing)

        backend.sync(syncConfig)
            .asSingle(coroutineContext)
            .doOnEvent { _, _ ->
                (syncState as Subject).onNext(SyncState.NotSyncing)
            }
            .subscribe(this::updateList, this::pushError)
            .addTo(compositeDisposable)
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }

    // there's probably a slicker way to do this `Unit` thing...
    private fun updateList(x: Unit) {
        if (!backend.isLocked()) {
            backend.list()
                .asSingle(coroutineContext)
                .subscribe(listSubject::accept, this::pushError)
                .addTo(compositeDisposable)
        }
    }

    private fun reset() {
        when (stateSubject.value) {
            null -> {
                stateSubject.onNext(State.Unprepared)
                return
            }
            State.Unprepared -> return
            else -> {
                clearList()
                backend.wipeLocal()
                    .asSingle(coroutineContext)
                    .map { State.Unprepared }
                    .subscribe(stateSubject::onNext, this::pushError)
                    .addTo(compositeDisposable)
            }
        }
    }

    private fun updateCredentials(credentials: SyncCredentials) {
        if (!credentials.isValid) {
            return
        }

        resetSupport(credentials.support)

        credentials.apply {
            support.syncConfig = SyncUnlockInfo(kid, accessToken.token, syncKey, tokenServerURL)
        }

        if (!credentials.isNew) {
            return
        }

        if (backend.isLocked()) {
            unlock()
        } else {
            stateSubject.onNext(State.Unlocked)
        }
    }

    private fun pushError(e: Throwable) {
        this.stateSubject.onNext(State.Errored(e))
    }

    // Warning: this is testing code.
    // It's only called immediately after the user has pressed "Use Test Data".
    fun resetSupport(support: DataStoreSupport) {
        if (support == this.support) {
            return
        }
        if (stateSubject.value != State.Unprepared &&
            stateSubject.value != null
        ) {
            backend.wipeLocal()
                .asSingle(coroutineContext)
                .subscribe()
                .addTo(compositeDisposable)
        }
        this.support = support
        this.backend = support.createLoginsStorage()
        // we shouldn't set the status of this to Unprepared,
        // as we don't want to change any UI.
    }
}

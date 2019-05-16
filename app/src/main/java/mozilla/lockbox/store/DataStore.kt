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
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.AutoLockSupport
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
open class DataStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    var support: DataStoreSupport? = null,
    private val autoLockSupport: AutoLockSupport = AutoLockSupport.shared,
    private val lifecycleStore: LifecycleStore = LifecycleStore.shared
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
    private var listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())

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

    private var backend: AsyncLoginsStorage?

    init {
        backend = support?.createLoginsStorage()
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
                    is DataStoreAction.Add -> add(action.item)
                }
            }
            .addTo(compositeDisposable)

        setupAutoLock()
    }

    private fun setupAutoLock() {
        val foreground = arrayOf(LifecycleAction.Foreground, LifecycleAction.AutofillStart)
        val background = arrayOf(LifecycleAction.Background, LifecycleAction.AutofillEnd)

        lifecycleStore.lifecycleEvents
            .filter { background.contains(it) && stateSubject.value == State.Unlocked }
            .subscribe {
                autoLockSupport.storeNextAutoLockTime()
            }
            .addTo(compositeDisposable)

        lifecycleStore.lifecycleEvents
            .filter { foreground.contains(it) }
            .map { autoLockSupport.shouldLock }
            .filter { it }
            .subscribe { lockInternal() }
            .addTo(compositeDisposable)
    }

    open fun get(id: String): Observable<Optional<ServerPassword>> {
        return list.map { items ->
            items.findLast { item -> item.id == id }.asOptional()
        }
    }

    private fun add(item: ServerPassword) {
        val backend = this.backend ?: return notReady()
        if (!backend.isLocked()) {
            backend.add(item)
                .asSingle(coroutineContext)
                .map { Unit }
                .subscribe(this::updateList, this::pushError)
                .addTo(compositeDisposable)
        }
    }

    private fun touch(id: String) {
        val backend = this.backend ?: return notReady()
        if (!backend.isLocked()) {
            backend.touch(id)
                .asSingle(coroutineContext)
                .subscribe(this::updateList, this::pushError)
                .addTo(compositeDisposable)
        }
    }

    private fun unlock() {
        // when we receive an external unlock action, assume it's not coming from autolock
        // and adjust our next autolocktime to avoid race condition with foregrounding / unlocking
        unlockInternal()
        autoLockSupport.forwardDateNextLockTime()
    }

    private fun unlockInternal() {
        val backend = this.backend ?: return notReady()
        val encryptionKey = support?.encryptionKey ?: return notReady()
        backend.ensureUnlocked(encryptionKey)
            .asSingle(coroutineContext)
            .toObservable()
            // start listening to the list when receiving the unlock completion
            .switchMap { list }
            // force an update
            .doOnNext { updateList(Unit) }
            // don't take the "locked" version of the list
            .skip(1)
            // once we get an "updated" list, we are done + can update the state
            .take(1)
            .map { State.Unlocked }
            .subscribe(stateSubject::onNext, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun lock() {
        lockInternal()
        autoLockSupport.backdateNextLockTime()
    }

    private fun lockInternal() {
        val backend = this.backend ?: return notReady()
        backend.ensureLocked()
            .asSingle(coroutineContext)
            .map { State.Locked }
            .subscribe(stateSubject::onNext, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun sync() {
        val syncConfig = support?.syncConfig ?: return {
            val throwable = IllegalStateException("syncConfig should already be defined")
            pushError(throwable)
        }()

        val backend = this.backend ?: return notReady()

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

    // Parameter x is needed to ensure that the function is indeed a Consumer so that it can be used in a suscribe-call
    // there's probably a slicker way to do this `Unit` thing...
    @Suppress("UNUSED_PARAMETER")
    private fun updateList(x: Unit) {
        val backend = this.backend ?: return notReady()
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
                backend?.let { backend ->
                    backend.wipeLocal()
                        .asSingle(coroutineContext)
                        .map { State.Unprepared }
                        .subscribe(stateSubject::onNext, this::pushError)
                        .addTo(compositeDisposable)
                } ?: stateSubject.onNext(State.Unprepared)
            }
        }
    }

    private fun updateCredentials(credentials: SyncCredentials) {
        if (!credentials.isValid) {
            return
        }

        resetSupport(credentials.support)
        val backend = this.backend ?: return notReady()

        credentials.apply {
            support.syncConfig = SyncUnlockInfo(kid, accessToken.token, syncKey, tokenServerURL)
        }

        if (!credentials.isNew) {
            if (autoLockSupport.shouldLock) {
                lockInternal()
            } else {
                unlockInternal()
            }
            return
        }

        if (backend.isLocked()) {
            unlockInternal()
        } else {
            stateSubject.onNext(State.Unlocked)
        }
    }

    private fun notReady() {
        pushError(IllegalStateException("DataStore backend is not set"))
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
            backend?.run {
                wipeLocal()
                    .asSingle(coroutineContext)
                    .subscribe()
                    .addTo(compositeDisposable)
            }
        }
        this.support = support
        this.backend = support.createLoginsStorage()
        // we shouldn't set the status of this to Unprepared,
        // as we don't want to change any UI.
    }
}

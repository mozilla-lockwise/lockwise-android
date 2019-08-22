/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import androidx.annotation.VisibleForTesting
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.ReplayRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asSingle
import mozilla.appservices.logins.InvalidKeyException
import mozilla.appservices.logins.LoginsStorageException
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncAuthInvalidException
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.sync.logins.AsyncLoginsStorage
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.Consumable
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.FxASyncDataStoreSupport
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.TimingSupport
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
open class DataStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    var support: DataStoreSupport = FxASyncDataStoreSupport.shared,
    private val timingSupport: TimingSupport = TimingSupport.shared,
    private val lifecycleStore: LifecycleStore = LifecycleStore.shared
) {
    companion object {
        val shared by lazy { DataStore() }
    }

    sealed class State {
        object Unprepared : State()
        object Locked : State()
        object Unlocked : State()
        data class Errored(val error: LoginsStorageException) : State()
    }

    enum class SyncState {
        Syncing, NotSyncing
    }

    internal val compositeDisposable = CompositeDisposable()
    private val stateSubject = ReplayRelay.createWithSize<State>(1)
    @VisibleForTesting
    val syncStateSubject: BehaviorRelay<SyncState> = BehaviorRelay.createDefault(SyncState.NotSyncing)
    private val listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())
    private val deletedItemSubject = ReplayRelay.create<Consumable<ServerPassword>>()

    open val state: Observable<State> = stateSubject
    open val syncState: Observable<SyncState> = syncStateSubject
    open val list: Observable<List<ServerPassword>> get() = listSubject
    open val deletedItem: Observable<Consumable<ServerPassword>> get() = deletedItemSubject

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

    private val foreground = arrayOf(LifecycleAction.Foreground, LifecycleAction.AutofillStart)
    private val background = arrayOf(LifecycleAction.Background, LifecycleAction.AutofillEnd)

    init {
        backend = support.createLoginsStorage()
        // handle state changes
        stateSubject
            .subscribe { state ->
                when (state) {
                    is State.Locked -> clearList()
                    is State.Unlocked -> syncIfRequired()
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
                    is DataStoreAction.Delete -> deleteCredentials(action.item)
                }
            }
            .addTo(compositeDisposable)

        lifecycleStore.lifecycleEvents
            .filter { this.background.contains(it) }
            .subscribe { this.shutdown() }
            .addTo(compositeDisposable)

        setupAutoLock()
    }

    private fun deleteCredentials(item: ServerPassword?) {
        try {
            if (item != null) {
                backend.delete(item.id)
                    .asSingle(coroutineContext)
                    .subscribe()
                    .addTo(compositeDisposable)
                sync()
                deletedItemSubject.accept(Consumable(item))
            }
        } catch (loginsStorageException: LoginsStorageException) {
            log.error("Exception: ", loginsStorageException)
        }
    }

    private fun editEntry() {
        // TODO
//        dispatcher.dispatch(RouteAction.ItemList)
    }

    private fun shutdown() {
        // rather than calling `close`, which will make the `AsyncLoginsStorage` instance unusable,
        // we use the `ensureLocked` method to close the database connection.
        backend.ensureLocked()
            .asSingle(coroutineContext)
            .subscribe()
            .addTo(compositeDisposable)
    }

    private fun setupAutoLock() {
        lifecycleStore.lifecycleEvents
            .filter { this.background.contains(it) && stateSubject.value == State.Unlocked }
            .subscribe {
                timingSupport.storeNextAutoLockTime()
            }
            .addTo(compositeDisposable)

        lifecycleStore.lifecycleEvents
            .filter { this.foreground.contains(it) && stateSubject.value != State.Unprepared }
            .subscribe { this.handleLock() }
            .addTo(compositeDisposable)
    }

    open fun get(id: String): Observable<Optional<ServerPassword>> {
        return list.map { items ->
            Optional(
                items.findLast { item -> item.id == id }
            )
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
        // when we receive an external unlock action, assume it's not coming from autolock
        // and adjust our next autolocktime to avoid race condition with foregrounding / unlocking
        unlockInternal()
        timingSupport.forwardDateNextLockTime()
    }

    private fun unlockInternal() {
        val encryptionKey = support.encryptionKey
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
            .subscribe(stateSubject::accept, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun lock() {
        lockInternal()
        timingSupport.backdateNextLockTime()
    }

    private fun lockInternal() {
        backend.ensureLocked()
            .asSingle(coroutineContext)
            .map { State.Locked }
            .subscribe(stateSubject::accept, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun handleLock() {
        if (timingSupport.shouldLock) {
            this.lockInternal()
        } else {
            this.unlockInternal()
        }
    }

    private fun syncIfRequired() {
        if (timingSupport.shouldSync) {
            this.sync()
            timingSupport.storeNextSyncTime()
        }
    }

    @VisibleForTesting(
        otherwise = VisibleForTesting.PRIVATE
    )
    fun sync() {
        resetSupport(support)

        val syncConfig = support.syncConfig ?: run {
            log.error("syncConfig is null in sync. This is likely a bug.")
            return
        }

        // ideally, we don't sync unless we are connected to the network
        syncStateSubject.accept(SyncState.Syncing)

        backend.sync(syncConfig)
            .asSingle(coroutineContext)
            .map {
                log.debug("Hashed UID: $it")
            }
            .timeout(Constant.App.syncTimeout, TimeUnit.SECONDS)
            .doOnEvent { _, err ->
                (err as? TimeoutException).let {
                    // syncStateSubject.accept(SyncState.TimedOut)
                    dispatcher.dispatch(DataStoreAction.SyncTimeout(it?.message ?: ""))
                }.run {
                    syncStateSubject.accept(SyncState.NotSyncing)
                }
            }
            .subscribe({
                this.updateList(it)
                dispatcher.dispatch(DataStoreAction.SyncSuccess)
            }, {
                this.pushError(it)
                dispatcher.dispatch(DataStoreAction.SyncError(it.message ?: ""))
            })
            .addTo(compositeDisposable)
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }

    // Parameter x is needed to ensure that the function is indeed a Consumer so that it can be used in a subscribe-call
    // there's probably a slicker way to do this `Unit` thing...
    @Suppress("UNUSED_PARAMETER")
    private fun updateList(x: Unit) {
        if (!backend.isLocked()) {
            backend.list()
                .asSingle(coroutineContext)
                .subscribe({
                    listSubject.accept(it)
                    dispatcher.dispatch(DataStoreAction.ListUpdate)
                }, {
                    this.pushError(it)
                    dispatcher.dispatch(DataStoreAction.ListUpdateError(it.message ?: ""))
                })
                .addTo(compositeDisposable)
        }
    }

    private fun reset() {
        when (stateSubject.value) {
            null -> {
                stateSubject.accept(State.Unprepared)
                return
            }
            State.Unprepared -> return
            else -> {
                clearList()
                backend.wipeLocal()
                    .asSingle(coroutineContext)
                    .map { State.Unprepared }
                    .subscribe(stateSubject::accept, this::pushError)
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
            this.handleLock()
            return
        }

        if (backend.isLocked()) {
            unlockInternal()
        } else {
            stateSubject.accept(State.Unlocked)
        }
    }

    private fun pushError(e: Throwable) {
        dispatcher.dispatch(SentryAction(e))

        val loginsException = e as? LoginsStorageException

        loginsException?.let {
            this.stateSubject.accept(State.Errored(it))
        }

        when (loginsException) {
            is SyncAuthInvalidException,
            is InvalidKeyException,
            is LoginsStorageException -> {
                dispatcher.dispatch(DataStoreAction.Errors(e.message ?: ""))
                resetSupport(support)
                dispatcher.dispatch(LifecycleAction.UserReset)
            }
        }
    }

    fun resetSupport(support: DataStoreSupport) {
        if (support == this.support) {
            return
        }

        if (stateSubject.value != State.Unprepared &&
            stateSubject.value != null
        ) {
            backend.wipeLocal()
                .asSingle(coroutineContext)
                .subscribe({}, this::pushError)
                .addTo(compositeDisposable)
        }
        this.support = support
        this.backend = support.createLoginsStorage()
        // we shouldn't set the status of this to Unprepared,
        // as we don't want to change any UI.
    }
}

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
import io.reactivex.subjects.Subject
import mozilla.lockbox.flux.Dispatcher
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.MemoryLoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncResult
import java.util.*


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
class DataStore(val dispatcher: Dispatcher = Dispatcher.shared) {
    internal val compositeDisposable = CompositeDisposable()
    private val stateSubject: ReplaySubject<DataStoreState> = ReplaySubject.create(1)
    private val listSubject: BehaviorRelay<List<ServerPassword>> = BehaviorRelay.createDefault(emptyList())

    private val backend: LoginsStorage

    init {
        // TODO: Replace test data with real backend
        backend = setupBackend()

        // handle state changes
        state.subscribe { state ->
            when (state.status) {
                DataStoreState.Status.LOCKED -> clearList()
                DataStoreState.Status.UNLOCKED -> updateList()
            }
        }.addTo(compositeDisposable)
    }

    val state: Observable<DataStoreState> get() = stateSubject

    val list: Observable<List<ServerPassword>> get() = listSubject

    fun unlock(): Observable<Unit> {
        val unlockSubject = SingleSubject.create<Unit>()

        backend.isLocked().then {
            if (it) {
                backend.unlock(getEncryptionKey()).then {
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

        backend.isLocked().then{
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

    // backend management
    private fun setupBackend(): LoginsStorage {
        val testdata = List<ServerPassword>(10) { createTestItem(it) }
        return MemoryLoginsStorage(testdata)
    }
    private fun loadInfo() {
        // TODO: load info from SharedPreferences ...

        backend.isLocked().whenComplete { result ->
            val status = if (result) DataStoreState.Status.LOCKED else DataStoreState.Status.UNLOCKED
            stateSubject.onNext(DataStoreState(status))
        }
    }
    private fun getEncryptionKey(): String {
        // TODO: read this from SharedPreferences
        return "keep-this-secret"
    }

    // item list management
    private fun clearList() {
        this.listSubject.accept(emptyList())
    }
    private fun updateList() {
        backend.list().whenComplete { all -> this.listSubject.accept(all) }
    }
}

internal fun createTestList(amt: Int = 10) : List<ServerPassword> {
    return List(amt) { createTestItem(it) }
}
/**
 * Creates a test ServerPassword item
 */
private fun createTestItem(idx: Int) : ServerPassword {
    val rng = Random()
    val pos = idx + 1
    val pwd = "AAbbcc112233!"
    val host = "https://$pos.example.com"
    val created = Date().time
    val used = Date(created - 86400000).time
    val changed = Date(used - 86400000).time

    return ServerPassword(
            id = "0000$idx",
            hostname= host,
            username = "someone #$pos",
            password = pwd,
            formSubmitURL = host,
            timeCreated = created,
            timeLastUsed = used,
            timePasswordChanged = changed,
            timesUsed = rng.nextInt(100) + 1
    )
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.flux.Dispatcher
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.MemoryLoginsStorage
import org.mozilla.sync15.logins.ServerPassword
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
        val testdata = List<ServerPassword>(10) { createTestItem(it) }
        backend = MemoryLoginsStorage(testdata)

        // TODO: Replace with state loaded from SharedPreferences ...
        stateSubject.onNext(DataStoreState(DataStoreState.Status.LOCKED))
    }

    val state: Observable<DataStoreState> get() = stateSubject

    val list: Observable<List<ServerPassword>> get() = listSubject
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

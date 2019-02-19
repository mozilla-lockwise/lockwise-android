/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Unlocking
import mozilla.lockbox.extensions.debug
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class LockedStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    private val lifecycleStore: LifecycleStore = LifecycleStore.shared
) {
    companion object {
        val shared = LockedStore()
    }

    open val onAuthentication: Observable<FingerprintAuthAction> =
        dispatcher.register
            .filterByType(FingerprintAuthAction::class.java)

    open val unlocking: Observable<Boolean> = BehaviorSubject.createDefault(false)
    open val forceLock: Observable<Boolean> = BehaviorSubject.createDefault(false)

    init {
        dispatcher.register
            .filterByType(DataStoreAction::class.java)
            .map { it == DataStoreAction.Lock }
            .subscribe(forceLock as Subject)

        lifecycleStore.lifecycleEvents
            .filter { it == LifecycleAction.Background }
            .map { false }
            .subscribe(forceLock as Subject)

        dispatcher.register
            .filterByType(Unlocking::class.java)
            .map { it.currently }
            .debug("new unlocking status")
            .subscribe(unlocking as Subject)
    }
}
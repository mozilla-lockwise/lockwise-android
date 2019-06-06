/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.Constant.App.delay
import java.util.concurrent.TimeUnit

open class AppLockedPresenter(
    lockedView: LockedView,
    override val dispatcher: Dispatcher = Dispatcher.shared,
    override val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    override val lockedStore: LockedStore = LockedStore.shared,
    open val settingStore: SettingStore = SettingStore.shared
) : LockedPresenter(lockedView, dispatcher, fingerprintStore, lockedStore) {

    override fun Observable<Unit>.unlockAuthenticationObservable(): Observable<Boolean> {
        return this.delay(delay, TimeUnit.SECONDS)
            .switchMap { lockedStore.canLaunchAuthenticationOnForeground.take(1) }
            .filter { it }
            .map { Unit }
            .mergeWith(lockedView.unlockButtonTaps)
            .doOnNext {
                dispatcher.dispatch(UnlockingAction(true))
            }
            .switchMap { settingStore.unlockWithFingerprint.take(1) }
    }
}
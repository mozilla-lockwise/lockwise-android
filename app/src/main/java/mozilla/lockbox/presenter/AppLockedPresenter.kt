/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.extensions.debug
import mozilla.lockbox.support.Constant.App.delay
import java.util.concurrent.TimeUnit

class AppLockedPresenter(
    lockedView: LockedView
) : LockedPresenter(lockedView) {

    override fun Observable<Unit>.unlockAuthenticationObservable(): Observable<Boolean> {
        return this.delay(delay, TimeUnit.SECONDS)
            .switchMap { lockedStore.canLaunchAuthenticationOnForeground.take(1) }
            .debug("canLaunchAuthOnForeground")
            .filter { it }
            .map { Unit }
            .mergeWith(lockedView.unlockButtonTaps)
            .doOnNext {
                dispatcher.dispatch(UnlockingAction(true))
            }
            .switchMap { settingStore.unlockWithFingerprint.take(1) }
    }
}
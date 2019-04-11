/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.support.Constant
import java.util.concurrent.TimeUnit

class AppLockedPresenter(
    lockedView: LockedView
) : LockedPresenter(lockedView) {

    override val launchAuthenticationObservable: Observable<Boolean> =
        Observable.just(Unit)
            .delay(Constant.App.delay, TimeUnit.SECONDS)
            .switchMap { lockedStore.canLaunchAuthenticationOnForeground.take(1) }
            .filter { it }
            .map { Unit }
            .mergeWith(lockedView.unlockButtonTaps ?: Observable.never())
            .doOnNext {
                dispatcher.dispatch(UnlockingAction(true))
            }
            .switchMap { settingStore.unlockWithFingerprint.take(1) }
}
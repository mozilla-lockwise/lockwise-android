/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class AutofillLockedPresenter(
    lockedView: LockedView
) : LockedPresenter(lockedView) {
    override val launchAuthenticationObservable: Observable<Boolean> =
        settingStore.unlockWithFingerprint
            .take(1)
            .delay(500, TimeUnit.MILLISECONDS)
}
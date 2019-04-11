/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import java.util.concurrent.TimeUnit

interface AutofillLockedView : LockedView

class AutofillLockedPresenter(
    lockedView: AutofillLockedView,
    lockedStore: LockedStore = LockedStore.shared,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : LockedPresenter(lockedView, dispatcher, fingerprintStore, lockedStore) {

    override fun onViewReady() {
        settingStore.unlockWithFingerprint
            .take(1)
            .delay(500, TimeUnit.MILLISECONDS)
            .subscribe {
                if (fingerprintStore.isFingerprintAuthAvailable && it) {
                    dispatcher.dispatch(RouteAction.DialogFragment.FingerprintDialog(R.string.fingerprint_dialog_title))
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)
        super.onViewReady()
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import java.util.concurrent.TimeUnit

interface AutofillLockedView {
    fun unlockFallback()
    val unlockConfirmed: Observable<Boolean>
}

class AutofillLockedPresenter(
    private val lockedView: AutofillLockedView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val lockedStore: LockedStore = LockedStore.shared
) : LockedPresenter() {
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

        lockedStore.onAuthentication
            .subscribe {
                when (it) {
                    is FingerprintAuthAction.OnSuccess -> unlock()
                    is FingerprintAuthAction.OnError -> unlockFallback()
                    is FingerprintAuthAction.OnCancel -> dispatcher.dispatch(AutofillAction.Cancel)
                }
            }
            .addTo(compositeDisposable)

        lockedView.unlockConfirmed
            .map {
                if (it) {
                    DataStoreAction.Unlock
                } else {
                    AutofillAction.Cancel
                }
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    override fun unlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
    }
}
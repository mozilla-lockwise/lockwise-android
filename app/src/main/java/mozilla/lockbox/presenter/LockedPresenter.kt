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
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore

interface LockedView {
    fun unlockFallback()
    val unlockConfirmed: Observable<Boolean>
    val unlockButtonTaps: Observable<Unit>?
}

abstract class LockedPresenter(
    private val lockedView: LockedView,
    val dispatcher: Dispatcher = Dispatcher.shared,
    val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    val lockedStore: LockedStore = LockedStore.shared,
    val settingStore: SettingStore = SettingStore.shared
) : Presenter() {

    abstract val launchAuthenticationObservable: Observable<Boolean>

    override fun onViewReady() {
        lockedView.unlockConfirmed
            .subscribe {
                if (it) {
                    unlock()
                } else {
                    dispatcher.dispatch(AutofillAction.Cancel)
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

        launchAuthenticationObservable
            .subscribe {
                if (fingerprintStore.isFingerprintAuthAvailable && it) {
                    dispatcher.dispatch(RouteAction.DialogFragment.FingerprintDialog(R.string.fingerprint_dialog_title))
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)
    }

    fun unlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        dispatcher.dispatch(UnlockingAction(false))
    }

    private fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            lockedView.unlockFallback()
        } else {
            unlock()
        }
    }
}
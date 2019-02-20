/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.isTesting
import java.util.concurrent.TimeUnit

interface LockedView {
    val unlockButtonTaps: Observable<Unit>
    fun unlockFallback()
    val unlockConfirmed: Observable<Boolean>
}

class LockedPresenter(
    private val view: LockedView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val lockedStore: LockedStore = LockedStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : Presenter() {
    private val delay: Long = if (isTesting()) 0 else 1
    override fun onViewReady() {
        // every time onViewReady is called, try to launch device authentication after 1 second
        Observable.just(Unit)
            .delay(delay, TimeUnit.SECONDS)
            .switchMap { lockedStore.canLaunchAuthenticationOnForeground.take(1) }
            .filter { it }
            .map { Unit }
            // make sure to listen for tapping the unlock button! it should always work.
            .mergeWith(view.unlockButtonTaps)
            // once we've started the unlock process, we don't want further foregrounding events to
            // launch the prompt again (i.e., cancelling your PIN entry to return to the unlock screen)
            .doOnNext {
                dispatcher.dispatch(UnlockingAction(true))
            }
            .switchMap { settingStore.unlockWithFingerprint.take(1) }
            .subscribe {
                if (fingerprintStore.isFingerprintAuthAvailable && it) {
                    dispatcher.dispatch(RouteAction.DialogFragment.FingerprintDialog(R.string.fingerprint_dialog_title))
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)

        view.unlockConfirmed
            .filter { it }
            .subscribe {
                unlock()
            }
            .addTo(compositeDisposable)

        lockedStore.onAuthentication
            .subscribe {
                if (it is FingerprintAuthAction.OnAuthentication) {
                    when (it.authCallback) {
                        is FingerprintAuthCallback.OnAuth -> unlock()
                        is FingerprintAuthCallback.OnError -> unlockFallback()
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    private fun unlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        dispatcher.dispatch(UnlockingAction(false))
    }

    private fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            view.unlockFallback()
        } else {
            unlock()
        }
    }
}

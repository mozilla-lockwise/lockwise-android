/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Unlocking
import mozilla.lockbox.extensions.debug
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
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
    override fun onViewReady() {
        val onViewReadyLaunch = Observable.just(Unit)
            .delay(1, TimeUnit.SECONDS)

        launchUnlockWithEvent(onViewReadyLaunch)
        alwaysLaunchUnlockWithEvent(view.unlockButtonTaps)

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
        dispatcher.dispatch(Unlocking(false))
    }

    private fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            view.unlockFallback()
        } else {
            unlock()
        }
    }

    private fun launchUnlockWithEvent(events: Observable<Unit>) {
        events
            .switchMap { Observables.combineLatest(lockedStore.unlocking, lockedStore.forceLock) }
            .take(1)
            .debug("unlocking?")
            .filter { !it.first && !it.second }
            .switchMap { settingStore.unlockWithFingerprint.take(1) }
            .doOnNext {
                dispatcher.dispatch(Unlocking(true))
            }
            .subscribe {
                if (fingerprintStore.isFingerprintAuthAvailable && it) {
                    dispatcher.dispatch(RouteAction.DialogFragment.FingerprintDialog(R.string.fingerprint_dialog_title))
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)
    }

    private fun alwaysLaunchUnlockWithEvent(events: Observable<Unit>) {
        events
            .switchMap { settingStore.unlockWithFingerprint.take(1) }
            .doOnNext {
                dispatcher.dispatch(Unlocking(true))
            }
            .subscribe {
                if (fingerprintStore.isFingerprintAuthAvailable && it) {
                    dispatcher.dispatch(RouteAction.DialogFragment.FingerprintDialog(R.string.fingerprint_dialog_title))
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)
    }
}

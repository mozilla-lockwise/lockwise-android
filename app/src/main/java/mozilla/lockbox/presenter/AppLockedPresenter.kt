/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.isTesting
import mozilla.lockbox.view.LockedView
import java.util.concurrent.TimeUnit

interface AppLockedView : LockedView {
    val unlockButtonTaps: Observable<Unit>
}

class AppLockedPresenter(
    private val lockedView: AppLockedView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val lockedStore: LockedStore = LockedStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : LockedPresenter(lockedView, dispatcher, fingerprintStore, lockedStore) {

    private val delay: Long = if (isTesting()) 0 else 1

    override fun onViewReady() {
        // every time onViewReady is called, try to launch device authentication after 1 second
        Observable.just(Unit)
            .delay(delay, TimeUnit.SECONDS)
            .switchMap { lockedStore.canLaunchAuthenticationOnForeground.take(1) }
            .filter { it }
            .map { Unit }
            // make sure to listen for tapping the unlock button! it should always work.
            .mergeWith(lockedView.unlockButtonTaps)
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

        super.onViewReady()
    }
}
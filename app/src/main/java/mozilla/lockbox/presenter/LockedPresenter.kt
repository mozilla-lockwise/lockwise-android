/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore

interface LockedView {
    fun unlockFallback()
    val unlockConfirmed: Observable<Boolean>
}

abstract class LockedPresenter(
    private val lockedView: LockedView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val lockedStore: LockedStore = LockedStore.shared
    ) : Presenter() {

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
    }

    fun unlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        dispatcher.dispatch(UnlockingAction(false))
    }

    fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            lockedView.unlockFallback()
        } else {
            unlock()
        }
    }
}
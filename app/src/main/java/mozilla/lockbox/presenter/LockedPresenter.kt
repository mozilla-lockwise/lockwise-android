/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment.AuthCallback

interface LockedView {
    val unlockButtonTaps: Observable<Unit>
    fun unlockFallback()
    val unlockConfirmed: Observable<Boolean>
}

class LockedPresenter(
    private val view: LockedView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val lockedStore: LockedStore = LockedStore.shared
) : Presenter() {
    override fun onViewReady() {
        view.unlockButtonTaps
            .subscribe {
                when {
                    fingerprintStore.isFingerprintAuthAvailable -> dispatcher.dispatch(RouteAction.FingerprintDialog)
                    fingerprintStore.isKeyguardDeviceSecure -> view.unlockFallback()
                    else -> performUnlock()
                }
            }
            .addTo(compositeDisposable)

        view.unlockConfirmed
            .subscribe {
                if (it) {
                    performUnlock()
                } else {
                    remainLocked()
                }
            }
            .addTo(compositeDisposable)

        lockedStore.onAuthentication
            .filterByType(FingerprintAuthAction.OnAuthentication::class.java)
            .subscribe {
                when (it.authCallback) {
                    is AuthCallback.OnAuth -> performUnlock()
                    is AuthCallback.OnError -> unlockFallback()
                }
            }
            .addTo(compositeDisposable)
    }

    private fun remainLocked() {
        dispatcher.dispatch(RouteAction.LockScreen)
    }

    private fun performUnlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
//        dispatcher.dispatch(RouteAction.ItemList)
    }

    private fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            view.unlockFallback()
        } else {
            performUnlock()
        }
    }
}

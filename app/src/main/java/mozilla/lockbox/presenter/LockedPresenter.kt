/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
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
                if (fingerprintStore.isFingerprintAuthAvailable) {
                    dispatcher.dispatch(RouteAction.FingerprintDialog)
                } else {
                    unlockFallback()
                }
            }
            .addTo(compositeDisposable)

        view.unlockConfirmed
            .subscribe {
                if (it) {
                    dispatcher.dispatch(RouteAction.ItemList)
                } else {
                    dispatcher.dispatch(RouteAction.LockScreen)
                }
            }
            .addTo(compositeDisposable)

        lockedStore.onAuthentication
            .subscribe {
                if (it is FingerprintAuthAction.OnAuthentication) {
                    when (it.authCallback) {
                        is AuthCallback.OnAuth -> dispatcher.dispatch(RouteAction.ItemList)
                        is AuthCallback.OnError -> unlockFallback()
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    private fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            view.unlockFallback()
        } else {
            dispatcher.dispatch(RouteAction.LockScreen)
        }
    }
}

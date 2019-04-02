/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore

abstract class LockedPresenter(
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    abstract fun unlock()

    fun unlockFallback() {
        if (fingerprintStore.isKeyguardDeviceSecure) {
            dispatcher.dispatch(RouteAction.UnlockFallbackDialog)
        } else {
            unlock()
        }
    }
}
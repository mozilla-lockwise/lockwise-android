/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.app.KeyguardManager
import io.reactivex.Observable
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class LockedStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    lateinit var keyguardManager: KeyguardManager

    companion object {
        val shared = LockedStore()
    }

    open val onAuthentication: Observable<FingerprintAuthAction> =
        dispatcher.register
            .filterByType(FingerprintAuthAction::class.java)

    fun apply(manager: KeyguardManager) {
        keyguardManager = manager
    }

    val isKeyguradSecure get() = keyguardManager.isKeyguardSecure
}
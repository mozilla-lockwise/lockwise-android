/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action
import mozilla.lockbox.model.FingerprintAuthCallback

sealed class FingerprintAuthAction : Action {
    data class OnAuthentication(val authCallback: FingerprintAuthCallback) : FingerprintAuthAction()
    object OnCancel : FingerprintAuthAction()
}
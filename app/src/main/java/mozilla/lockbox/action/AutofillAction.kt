/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.flux.Action

sealed class AutofillAction : Action {
    data class Complete(val login: ServerPassword) : AutofillAction()
    data class CompleteMultiple(val logins: List<ServerPassword>) : AutofillAction()
    data class Error(val error: Throwable) : AutofillAction()
    object Authenticate : AutofillAction()
    object Cancel : AutofillAction()
}
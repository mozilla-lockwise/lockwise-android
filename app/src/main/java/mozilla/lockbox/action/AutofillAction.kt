/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.appservices.logins.ServerPassword

sealed class AutofillAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    data class Complete(val login: ServerPassword) : AutofillAction(TelemetryEventMethod.autofill_single, TelemetryEventObject.autofill)
    data class CompleteMultiple(val logins: List<ServerPassword>) : AutofillAction(TelemetryEventMethod.autofill_multiple, TelemetryEventObject.autofill)
    data class Error(val error: Throwable) : AutofillAction(TelemetryEventMethod.autofill_error, TelemetryEventObject.autofill)
    object SearchFallback : AutofillAction(TelemetryEventMethod.autofill_filter, TelemetryEventObject.autofill)
    object Authenticate : AutofillAction(TelemetryEventMethod.autofill_locked, TelemetryEventObject.autofill)
    object Cancel : AutofillAction(TelemetryEventMethod.autofill_cancel, TelemetryEventObject.autofill)
}
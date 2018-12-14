/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

sealed class ClipboardAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    data class CopyUsername(val username: String) : ClipboardAction(TelemetryEventMethod.tap, TelemetryEventObject.entry_copy_username_button)
    data class CopyPassword(val password: String) : ClipboardAction(TelemetryEventMethod.tap, TelemetryEventObject.entry_copy_password_button)
}

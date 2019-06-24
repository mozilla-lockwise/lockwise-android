/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.appservices.logins.ServerPassword

sealed class ItemDetailAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {

    data class TogglePassword(val displayed: Boolean)
        : ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.reveal_password)
    data class Delete(val itemId: String?)
        : ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.delete_credential)
}

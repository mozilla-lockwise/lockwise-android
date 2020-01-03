/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

sealed class ItemDetailAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    data class SetPasswordVisibility(val visible: Boolean) :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.reveal_password)

    data class BeginEditItemSession(val itemId: String) :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.begin_edit_item_session)
    object EditItemSaveChanges :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.update_credential)
    object EndEditItemSession :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.end_edit_item_session)

    object BeginCreateItemSession :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.begin_manual_create_session)
    object CreateItemSaveChanges :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.manual_create_save)
    object EndCreateItemSession :
        ItemDetailAction(TelemetryEventMethod.tap, TelemetryEventObject.end_manual_create_session)

    data class EditField(
        val username: String? = null,
        val password: String? = null,
        val hostname: String? = null
    ) : ItemDetailAction(TelemetryEventMethod.edit, TelemetryEventObject.update_credential)
}

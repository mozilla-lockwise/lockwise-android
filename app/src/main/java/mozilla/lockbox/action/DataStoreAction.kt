/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.model.SyncCredentials

sealed class DataStoreAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject,
    override val value: String? = null,
    override val extras: Map<String, Any>? = null
) : TelemetryAction {
    object Lock : DataStoreAction(TelemetryEventMethod.lock, TelemetryEventObject.datastore)
    object Unlock : DataStoreAction(TelemetryEventMethod.unlock, TelemetryEventObject.datastore)
    object Reset : DataStoreAction(TelemetryEventMethod.reset, TelemetryEventObject.datastore)

    /**
     * Emitted when the app requests a sync.
     */
    object Sync : DataStoreAction(TelemetryEventMethod.sync_start, TelemetryEventObject.datastore)

    /**
     * Emitted when a sync request completes successfully with sync data.
     */
    object SyncSuccess : DataStoreAction(TelemetryEventMethod.sync_end, TelemetryEventObject.datastore)

    /**
     * Emitted when the app times out when listening for a response from sync.
     */
    data class SyncTimeout(val error: String) : DataStoreAction(
        TelemetryEventMethod.sync_timeout,
        TelemetryEventObject.datastore,
        error
    )

    /**
     * Emitted when the app receives an error response from sync.
     */
    data class SyncError(val error: String) : DataStoreAction(
        TelemetryEventMethod.sync_error,
        TelemetryEventObject.datastore,
        error
    )

    /**
     * Emitted when the item list has been updated.
     */
    object ListUpdate : DataStoreAction(TelemetryEventMethod.list_update, TelemetryEventObject.datastore)

    /**
     * Emitted when an update to the item list has failed with an error.
     */
    data class ListUpdateError(val error: String) : DataStoreAction(
        TelemetryEventMethod.list_update_error,
        TelemetryEventObject.datastore,
        error
    )

    /**
     * Emitted when a DataStore action results in an error.
     */
    data class Errors(val error: String) : DataStoreAction(
        TelemetryEventMethod.list_update_error,
        TelemetryEventObject.datastore,
        error
    )

    data class Touch(val id: String) : DataStoreAction(
        TelemetryEventMethod.touch,
        TelemetryEventObject.datastore,
        "ID: $id"
    )

    data class UpdateCredentials(val syncCredentials: SyncCredentials) : DataStoreAction(
        TelemetryEventMethod.update_credentials,
        TelemetryEventObject.datastore
    )

    data class Delete(val item: ServerPassword?) : DataStoreAction(
        TelemetryEventMethod.delete,
        TelemetryEventObject.delete_credential,
        item?.id
    )

    data class Edit(val itemId: Int) : DataStoreAction(TelemetryEventMethod.edit, TelemetryEventObject.edit_credential)
}

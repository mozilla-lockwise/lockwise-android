/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.model.AutofillItemViewModel
import mozilla.lockbox.model.SyncCredentials

sealed class DataStoreAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    object Lock : DataStoreAction(TelemetryEventMethod.lock, TelemetryEventObject.datastore)
    object Unlock : DataStoreAction(TelemetryEventMethod.unlock, TelemetryEventObject.datastore)
    object Reset : DataStoreAction(TelemetryEventMethod.reset, TelemetryEventObject.datastore)
    object Sync : DataStoreAction(TelemetryEventMethod.sync, TelemetryEventObject.datastore)
    data class Add(val item: AutofillItemViewModel) : DataStoreAction(TelemetryEventMethod.autofill_add, TelemetryEventObject.datastore)
    data class Touch(val id: String) : DataStoreAction(TelemetryEventMethod.touch, TelemetryEventObject.datastore)
    data class UpdateCredentials(val syncCredentials: SyncCredentials) : DataStoreAction(TelemetryEventMethod.update_credentials, TelemetryEventObject.datastore)
}

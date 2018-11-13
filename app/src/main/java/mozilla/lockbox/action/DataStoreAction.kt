/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

sealed class DataStoreAction : Action {
    object Lock : DataStoreAction()
    object Unlock : DataStoreAction()
    object Reset : DataStoreAction()
    object Sync : DataStoreAction()
    data class Touch(val id: String) : DataStoreAction()
}

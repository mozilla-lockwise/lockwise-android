/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action
import mozilla.lockbox.model.ItemListSort

open class SettingAction : Action {
    data class SendUsageData(val sendUsageData: Boolean) : SettingAction()
    data class ItemListSortOrder(val sortOrder: ItemListSort) : SettingAction()
}
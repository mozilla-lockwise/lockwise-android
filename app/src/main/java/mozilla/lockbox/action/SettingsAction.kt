/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.R
import mozilla.lockbox.flux.Action

sealed class SettingsAction : Action {
    data class SortAction(val id: ItemListSort) : SettingsAction()
}

enum class ItemListSort(val sortId: Int, val displayStringId: Int) {
    ALPHABETICALLY(0, R.string.sort_menu_az),
    RECENTLY_USED(1, R.string.sort_menu_recent);

    // This handy little snippit for creation of enums by value comes courtesy of
    // <a href="https://stackoverflow.com/a/37795810">JB Nizet</a>
    companion object {
        private val idMap = ItemListSort.values().associateBy(ItemListSort::sortId)
        fun fromSortId(index: Int) = idMap[index]
    }
}
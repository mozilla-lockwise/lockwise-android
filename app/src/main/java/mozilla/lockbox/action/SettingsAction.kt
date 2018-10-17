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

enum class ItemListSort(val sortId: Int) {
    ALPHABETICALLY(R.id.sort_a_z),
    RECENTLY_USED(R.id.sort_recent);

    // This handy little snippit for creation of enums by value comes courtesy of
    // <a href="https://stackoverflow.com/a/37795810">JB Nizet</a>
    companion object {
        private val map = ItemListSort.values().associateBy(ItemListSort::sortId)
        fun fromInt(sortId: Int) = map[sortId]

        fun idFromOrdinal(ordinal: Int): Int {
            return when (ordinal) {
                RECENTLY_USED.ordinal -> RECENTLY_USED.sortId
                else -> ALPHABETICALLY.sortId
            }
        }
    }
}
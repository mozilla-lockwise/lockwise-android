/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.R
import mozilla.lockbox.flux.Action

sealed class SortAction : Action {
    object Alphabetically : SortAction()
    object RecentlyUsed : SortAction()
}

enum class ItemListSort {
    ALPHABETICALLY,
    RECENTLY_USED;

    companion object {
        fun idFromOrdinal(ordinal: Int): Int {
            return when (ordinal) {
                ALPHABETICALLY.ordinal -> R.id.sort_a_z
                RECENTLY_USED.ordinal -> R.id.sort_recent
                else -> R.id.sort_a_z
            }
        }
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import mozilla.lockbox.R

enum class ItemListSort(val displayStringId: Int) {
    ALPHABETICALLY(R.string.sort_menu_az),
    RECENTLY_USED(R.string.sort_menu_recent);
}
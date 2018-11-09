/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.model.ItemListSort

object Constant {
    object App {
        const val keystoreLabel = "lockbox-keystore"
    }

    object FxA {
        const val clientID = "e7ce535d93522896"
        const val redirectUri = "https://lockbox.firefox.com/fxa/android-redirect.html"
        val scopes = arrayOf(
            "profile",
            "https://identity.mozilla.com/apps/lockbox",
            "https://identity.mozilla.com/apps/oldsync"
        )
    }

    object Setting {
        val defaultItemListSort = ItemListSort.ALPHABETICALLY
        val defaultSendUsageData = true
    }
}
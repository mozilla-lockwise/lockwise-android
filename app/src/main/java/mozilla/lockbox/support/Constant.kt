/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.action.Setting

object Constant {
    object App {
        const val keystoreLabel = "lockbox-keystore"
        const val dbFilename = "firefox-lockbox.db"
        const val testMarker = "TEST"
    }

    object FxA {
        const val clientID = "e7ce535d93522896"
        const val redirectUri = "https://lockbox.firefox.com/fxa/android-redirect.html"
        const val oldSyncScope = "https://identity.mozilla.com/apps/oldsync"
        const val lockboxScope = "https://identity.mozilla.com/apps/lockbox"
        const val profileScope = "profile"

        val scopes = arrayOf(profileScope, lockboxScope, oldSyncScope)
    }

    object Faq {
        const val uri = "https://mozilla-lockbox.github.io/lockbox-android/"
    }

    object SettingDefault {
        val itemListSort = Setting.ItemListSort.ALPHABETICALLY
        val autoLockTime = Setting.AutoLockTime.FiveMinutes
        const val sendUsageData = true
        const val unlockWithFingerprint = false
    }

    object Key {
        const val firefoxAccount = "firefox-account"
        const val encryptionKey = "database-encryption-key"
    }
}

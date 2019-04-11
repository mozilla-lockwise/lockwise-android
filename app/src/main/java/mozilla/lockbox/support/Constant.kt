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
        const val bootIDPath = "/proc/sys/kernel/random/boot_id"
        const val appToken = "383z4i46o48w"

        val delay: Long = if (isTesting()) 0 else 1
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
        const val uri = "https://lockbox.firefox.com/faq.html"
        const val topUri = uri + "#top"
        const val savedUri = uri + "#how-do-i-get-my-saved-logins-into-firefox-lockbox"
        const val securityUri = uri + "#what-security-technologies-does-firefox-lockbox-use"
        const val syncUri = uri + "#how-do-i-enable-sync-on-firefox"
        const val createUri = uri + "#how-do-i-create-new-entries"
        const val editUri = uri + "#how-do-i-edit-existing-entries"
    }

    object Privacy {
        const val uri = "https://lockbox.firefox.com/privacy.html"
    }

    object SendFeedback {
        const val uri = "https://www.surveygizmo.com/s3/4713557/Provide-feedback-for-Firefox-Lockbox-Android"
    }

    object SettingDefault {
        val itemListSort = Setting.ItemListSort.ALPHABETICALLY
        val autoLockTime = Setting.AutoLockTime.FiveMinutes
        val noSecurityAutoLockTime = Setting.AutoLockTime.Never
        const val sendUsageData = true
        const val unlockWithFingerprint = false
    }

    object Key {
        const val firefoxAccount = "firefox-account"
        const val encryptionKey = "database-encryption-key"
        const val autoLockTimerDate = "auto-lock-timer-date"
    }

    object FingerprintTimeout {
        const val errorTimeoutMillis: Long = 1600
        const val successDelayMillis: Long = 1300
    }

    object RequestCode {
        const val noResult = 0
        const val lock = 112
        const val unlock = 221
    }

    object Sentry {
        const val dsn = "https://19558af5301f43e1a95ab4b8ceae663b@sentry.prod.mozaws.net/401"
    }
}

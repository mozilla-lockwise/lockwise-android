/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.action.Setting

object Constant {
    object Common {
        const val emptyString = ""
        const val sixtySeconds: Long = 60000
        const val twentyFourHours: Long = 60 * 60 * 24 * 1000
    }

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

        val scopes = setOf(profileScope, lockboxScope, oldSyncScope)
    }

    object Faq {
        const val uri = "https://support.mozilla.org/en-US/kb"
        const val faqUri = uri + "/getting-started-firefox-lockwise"
        const val syncUri = uri + "/sync-logins-firefox-android"
    }

    object Privacy {
        const val uri = "https://support.mozilla.org/en-US/kb/firefox-lockwise-and-privacy"
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
        const val syncTimerDate = "sync-timer-date"
        const val bootCompletedIntent = "android.intent.action.BOOT_COMPLETED"
        const val clearClipboardIntent = "mozilla.lockbox.intent.CLEAR_CLIPBOARD"
        const val clipboardDirtyExtra = "clipboard-dirty"
        const val accessToken = "access-token"
        const val parsedStructure = "parsed-structure"
    }

    object FingerprintTimeout {
        const val errorTimeoutMillis: Long = 1600
        const val successDelayMillis: Long = 1300
    }

    object RequestCode {
        const val noResult = 0
        const val unlock = 221
    }

    object Sentry {
        const val dsn = "https://19558af5301f43e1a95ab4b8ceae663b@sentry.prod.mozaws.net/401"
    }
}

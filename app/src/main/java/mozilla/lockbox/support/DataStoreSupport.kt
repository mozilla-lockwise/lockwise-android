/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.MemoryLoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncUnlockInfo
import java.util.Date
import java.util.Random

interface DataStoreSupport {
    val encryptionKey: String
    val syncConfig: SyncUnlockInfo
    fun createLoginsStorage(): LoginsStorage
}

// Fixed-Data Implementation
class FixedDataStoreSupport(values: List<ServerPassword>? = null) : DataStoreSupport {
    private val logins = MemoryLoginsStorage(values ?: List(10) { createDummyItem(it) })

    override val encryptionKey: String
        get() = "shh-keep-it-secret"
    override val syncConfig: SyncUnlockInfo
        get() = SyncUnlockInfo(
                kid = "fake-kid",
                fxaAccessToken = "fake-at",
                syncKey = "fake-key",
                tokenserverURL = "https://oauth.example.com/token"
        )

    override fun createLoginsStorage(): LoginsStorage = logins

    companion object {
        val shared = FixedDataStoreSupport()
    }
}

/**
 * Creates a test ServerPassword item
 */
internal fun createDummyItem(idx: Int): ServerPassword {
    val rng = Random()
    val pos = idx + 1
    val pwd = "AAbbcc112233!"
    val host = "https://$pos.example.com"
    val created = Date().time
    val used = Date(created - 86400000).time
    val changed = Date(used - 86400000).time

    return ServerPassword(
            id = "0000$idx",
            hostname = host,
            username = "someone #$pos",
            password = pwd,
            formSubmitURL = host,
            timeCreated = created,
            timeLastUsed = used,
            timePasswordChanged = changed,
            timesUsed = rng.nextInt(100) + 1
    )
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import org.mozilla.sync15.logins.DatabaseLoginsStorage
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.SyncUnlockInfo
import java.util.UUID.randomUUID

private const val encryptionKeyKey: String = "database-encryption-key"
private const val dbPath = "my-first-db.db"

class FxASyncDataStoreSupport(
    private val preferences: SecurePreferences = SecurePreferences.shared
) : DataStoreSupport {

    override var syncConfig: SyncUnlockInfo? = null

    override val encryptionKey by lazy {
        preferences.getString(encryptionKeyKey)?.let {
            return@lazy it
        }

        val encryptionKey = randomUUID().toString()
        preferences.putString(encryptionKeyKey, encryptionKey)

        encryptionKey
    }

    fun wipe() {
        preferences.remove(encryptionKeyKey)
    }

    override fun createLoginsStorage(): LoginsStorage = DatabaseLoginsStorage(dbPath)

}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import mozilla.appservices.logins.DatabaseLoginsStorage
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.sync.logins.AsyncLoginsStorage
import mozilla.components.service.sync.logins.AsyncLoginsStorageAdapter
import mozilla.lockbox.store.ContextStore
import java.security.SecureRandom

class FxASyncDataStoreSupport(
    private val preferences: SecurePreferences = SecurePreferences.shared
) : DataStoreSupport, ContextStore {

    companion object {
        val shared = FxASyncDataStoreSupport()
    }

    private lateinit var dbFilePath: String

    override var syncConfig: SyncUnlockInfo? = null

    override val encryptionKey by lazy {
        preferences.getString(Constant.Key.encryptionKey)?.let {
            return@lazy it
        }

        // val encryptionKey = randomUUID().toString()
        val encryptionKey = generateRandomString(32)
        preferences.putString(Constant.Key.encryptionKey, encryptionKey)

        encryptionKey
    }

    private fun generateRandomString(keyLength: Int): String {
        val bytes = ByteArray(keyLength * 2)
        val random = SecureRandom()
        random.nextBytes(bytes)

        return bytes
            .map {
                val full = it.toInt()
                val hi = (full and 0xf0) ushr 4
                val lo = full and 0x0f
                "%h%h".format(hi, lo)
            }
            .joinToString("")
    }

    override fun createLoginsStorage(): AsyncLoginsStorage = AsyncLoginsStorageAdapter(DatabaseLoginsStorage(dbFilePath))

    override fun injectContext(context: Context) {
        dbFilePath = context.getDatabasePath(Constant.App.dbFilename).absolutePath
    }
}
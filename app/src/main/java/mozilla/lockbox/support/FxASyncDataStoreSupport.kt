/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import mozilla.lockbox.store.ContextStore
import org.mozilla.sync15.logins.DatabaseLoginsStorage
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.SyncUnlockInfo
import java.util.UUID.randomUUID

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

        val encryptionKey = randomUUID().toString()
        preferences.putString(Constant.Key.encryptionKey, encryptionKey)

        encryptionKey
    }

    override fun createLoginsStorage(): LoginsStorage = DatabaseLoginsStorage(dbFilePath)

    override fun injectContext(context: Context) {
        dbFilePath = context.getDatabasePath(Constant.App.dbFilename).absolutePath
    }
}
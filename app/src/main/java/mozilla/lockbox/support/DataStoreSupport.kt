/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.SyncUnlockInfo

interface DataStoreSupport {
    val encryptionKey: String
    var syncConfig: SyncUnlockInfo?
    fun createLoginsStorage(): LoginsStorage
}
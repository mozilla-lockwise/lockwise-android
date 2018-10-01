/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.mocks

import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.createDummyItem
import org.mockito.Mockito
import org.mozilla.sync15.logins.IdCollisionException
import org.mozilla.sync15.logins.InvalidRecordException
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.NoSuchRecordException
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncResult
import org.mozilla.sync15.logins.SyncUnlockInfo

open class MockLoginsStorage : LoginsStorage {
    private val all = MutableList(10) { createDummyItem(it) }

    private var _locked = true

    override fun isLocked(): SyncResult<Boolean> = SyncResult.fromValue(_locked)

    override fun list(): SyncResult<List<ServerPassword>> {
        return checkLockState().then {
            SyncResult.fromValue(all.toList())
        }
    }
    override fun get(id: String): SyncResult<ServerPassword?> {
        return checkLockState().then { _ ->
            SyncResult.fromValue(all.find { it.id == id })
        }
    }
    override fun touch(id: String): SyncResult<Unit> = checkLockState()
    override fun delete(id: String): SyncResult<Boolean> {
        return checkLockState().then {
            get(id)
        }.then {
            if (it == null) {
                SyncResult.fromValue(false)
            }
            all.removeAt(all.indexOf(it))
            SyncResult.fromValue(true)
        }
    }
    override fun add(login: ServerPassword): SyncResult<String> {
        return list().then {
            if (login.id == "") {
                SyncResult.fromException<String>(InvalidRecordException("record id missing"))
            } else if (null != it.find { it.id == login.id }) {
                SyncResult.fromException<String>(IdCollisionException("record already exists"))
            }

            all.add(login)
            SyncResult.fromValue(login.id)
        }
    }

    override fun update(login: ServerPassword): SyncResult<Unit> {
        return list().then {
            val position = it.indexOfFirst { it.id == login.id }
            if (position == -1) {
                SyncResult.fromException(NoSuchRecordException("record not found"))
            } else {
                all[position] = login
                SyncResult.fromValue(Unit)
            }
        }
    }

    override fun lock(): SyncResult<Unit> {
        return SyncResult.fromValue(_locked).then {
            if (it) {
                SyncResult.fromException(IllegalStateException("storage already locked!"))
            } else {
                _locked = true
                SyncResult.fromValue(Unit)
            }
        }
    }
    override fun unlock(encryptionKey: String): SyncResult<Unit> {
        return SyncResult.fromValue(_locked).then {
            if (!it) {
                SyncResult.fromException(IllegalStateException("storage already unlocked!"))
            } else {
                _locked = false
                SyncResult.fromValue(Unit)
            }
        }
    }
    override fun close() { }

    override fun sync(syncInfo: SyncUnlockInfo): SyncResult<Unit> {
        return checkLockState().then {
            SyncResult.fromValue(Unit)
        }
    }

    override fun reset(): SyncResult<Unit> {
        return SyncResult.fromValue(Unit)
    }

    override fun wipe(): SyncResult<Unit> {
        all.clear()
        return SyncResult.fromValue(Unit)
    }

    private fun checkLockState(): SyncResult<Unit> = if (_locked)
        SyncResult.fromException(IllegalStateException("storage is locked!"))
            else SyncResult.fromValue(Unit)
}

class MockDataStoreSupport : DataStoreSupport {
    val storage = Mockito.spy<MockLoginsStorage>(MockLoginsStorage())

    override val encryptionKey = "testing-key"
    override val syncConfig = SyncUnlockInfo("mock-kid", "mock-at", "mock-synckey", "https://mock.example.com/oauth/token")

    override fun createLoginsStorage(): LoginsStorage = storage
}

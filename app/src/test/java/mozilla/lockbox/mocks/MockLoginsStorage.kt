/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.mocks

import mozilla.appservices.logins.LoginsStorage
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.appservices.sync15.SyncTelemetryPing
import mozilla.components.service.sync.logins.AsyncLoginsStorage
import mozilla.components.service.sync.logins.AsyncLoginsStorageAdapter
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.createDummyItem
import org.mockito.Mockito

open class MockLoginsStorage : LoginsStorage {
    private var all = MutableList(10) { createDummyItem() }

    private var _locked = true

    override fun isLocked(): Boolean = _locked

    override fun list(): List<ServerPassword> = all.toList()

    override fun get(id: String): ServerPassword? = all.find { it.id == id }
    override fun touch(id: String) {}
    override fun delete(id: String): Boolean {
        return true
    }

    override fun add(login: ServerPassword): String {
        all.add(login)
        return ""
    }

    override fun update(login: ServerPassword) {
    }

    override fun lock() {
        this._locked = true
    }
    override fun ensureLocked() {
        this._locked = true
    }

    override fun unlock(encryptionKey: String) {
        this._locked = false
    }
    override fun unlock(encryptionKey: ByteArray) {
        this._locked = false
    }
    override fun ensureUnlocked(encryptionKey: String) {
        this._locked = false
    }

    override fun ensureValid(login: ServerPassword) {
    }

    override fun ensureUnlocked(encryptionKey: ByteArray) {
        this._locked = false
    }

    override fun close() {}

    override fun sync(syncInfo: SyncUnlockInfo): SyncTelemetryPing {
        return SyncTelemetryPing(
            version = 1,
            uid = "uid",
            events = emptyList(),
            syncs = emptyList()
        )
    }

    override fun reset() {}

    override fun wipe() {}

    override fun wipeLocal() {}

    override fun getByHostname(hostname: String): List<ServerPassword> {
        return all.filter { hostname == it.hostname }
    }

    override fun getHandle(): Long {
        throw UnsupportedOperationException()
    }

    override fun importLogins(logins: Array<ServerPassword>): Long {
        all.addAll(logins)
        return 0 // no errors
    }
}

class MockDataStoreSupport : DataStoreSupport {
    val storage = Mockito.spy<MockLoginsStorage>(MockLoginsStorage())
    val asyncStorage = Mockito.spy(AsyncLoginsStorageAdapter(storage))

    override val encryptionKey = "testing-key"
    override var syncConfig: SyncUnlockInfo? =
        SyncUnlockInfo("mock-kid", "mock-at", "mock-synckey", "https://mock.example.com/oauth/token")

    override fun createLoginsStorage(): AsyncLoginsStorage = asyncStorage
}

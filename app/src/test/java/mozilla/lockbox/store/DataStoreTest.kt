/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.DataStoreSupport
import mozilla.lockbox.support.createDummyItem
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mozilla.sync15.logins.LoginsStorage
import org.mozilla.sync15.logins.ServerPassword
import org.mozilla.sync15.logins.SyncResult
import org.mozilla.sync15.logins.SyncUnlockInfo

class MockLoginsStorage : LoginsStorage {
    private val all = MutableList<ServerPassword>(10) { createDummyItem(it) }

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

    override fun reset(): SyncResult<Unit> = SyncResult.fromValue(Unit)

    override fun wipe(): SyncResult<Unit> {
        all.clear()
        return SyncResult.fromValue(Unit)
    }

    private fun checkLockState(): SyncResult<Unit> = if (_locked)
            SyncResult.fromException(IllegalStateException("storage is locked!"))
            else SyncResult.fromValue(Unit)
}

class MockDataStoreSupport : DataStoreSupport {
    override val encryptionKey: String
        get() = "testing-key"

    override fun createLoginsStorage(): LoginsStorage = MockLoginsStorage()
}

class DataStoreTest : DisposingTest() {
    private val support = MockDataStoreSupport()

    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: DataStore

    @Before
    fun setUp() {
        // TODO: mock backend for testing ...
        val dispatcher = Dispatcher()
        this.dispatcher = dispatcher
        subject = DataStore(dispatcher, support)
    }

    @Test
    fun testInitialState() {
        Assert.assertSame(dispatcher, subject.dispatcher)
        subject.list.subscribe {
            Assert.assertTrue(it.isEmpty())
        }.addTo(disposer)
        subject.state.subscribe {
            Assert.assertEquals(DataStoreState.Status.LOCKED, it.status)
            Assert.assertNull(it.error)
        }.addTo(disposer)
    }

    @Test
    fun testLockUnlock() {
        val stateObserver = createTestObserver<DataStoreState>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        stateObserver.values()
        var waiter = createTestObserver<Unit>()
        subject.unlock().subscribe(waiter)
        waiter.assertComplete()

        waiter = createTestObserver<Unit>()
        subject.lock().subscribe(waiter)
        waiter.assertComplete()

        stateObserver.apply {
            // TODO: figure out why the initialized state isn't here?
            assertValueCount(2)
            assertValueAt(0, DataStoreState(DataStoreState.Status.UNLOCKED))
            assertValueAt(1, DataStoreState(DataStoreState.Status.LOCKED))
        }
        listObserver.apply {
            val results = values()
            Assert.assertEquals(3, results.size)
            Assert.assertEquals(0, results[0].size)
            Assert.assertEquals(10, results[1].size)
            Assert.assertEquals(0, results[2].size)
        }
    }

    @Test
    fun testActionHandling() {
        val stateObserver = createTestObserver<DataStoreState>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        dispatcher.dispatch(DataStoreAction(DataStoreAction.Type.UNLOCK))
        dispatcher.dispatch(DataStoreAction(DataStoreAction.Type.LOCK))
        stateObserver.apply {
            assertValueCount(2)
            assertValueAt(0, DataStoreState(DataStoreState.Status.UNLOCKED))
            assertValueAt(1, DataStoreState(DataStoreState.Status.LOCKED))
        }
    }

    private fun <T> createTestObserver(): TestObserver<T> {
        val result = TestObserver.create<T>()
        result.addTo(disposer)
        return result
    }
}

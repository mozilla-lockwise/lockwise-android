/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.store.DataStore.State
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mozilla.sync15.logins.ServerPassword
import java.lang.IllegalStateException

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
            Assert.assertEquals(State.Unprepared, it)
        }.addTo(disposer)
    }

    @Test
    fun testLockUnlock() {
        val stateObserver = createTestObserver<State>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        stateObserver.values()
        var waiter = createTestObserver<Unit>()
        subject.unlock().subscribe(waiter)
        waiter.assertComplete()

        waiter = createTestObserver()
        subject.lock().subscribe(waiter)
        waiter.assertComplete()

        stateObserver.apply {
            // TODO: figure out why the initialized state isn't here?
            assertValueCount(3)
            assertValueAt(0, State.Unprepared)
            assertValueAt(1, State.Unlocked)
            assertValueAt(2, State.Locked)
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
        val stateObserver = createTestObserver<State>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Mockito.verify(support.storage).unlock(support.encryptionKey)
        Mockito.verify(support.storage).list()
        Mockito.clearInvocations(support.storage)

        dispatcher.dispatch(DataStoreAction.Sync)
        Mockito.verify(support.storage).sync(support.syncConfig!!)
        Mockito.verify(support.storage).list()
        Mockito.clearInvocations(support.storage)

        dispatcher.dispatch(DataStoreAction.Lock)
        Mockito.verify(support.storage).lock()
        Mockito.clearInvocations(support.storage)
    }

    @Test
    fun testTouch() {
        val stateObserver = createTestObserver<State>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        support.storage.unlock("fdsfsdfds")

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        val id = "lkjhkj"
        dispatcher.dispatch(DataStoreAction.Touch(id))

        Mockito.verify(support.storage).touch(id)
        Mockito.verify(support.storage).list()
    }

    @Test
    fun testReset() {
        val stateObserver = createTestObserver<State>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        support.storage.unlock("fdsfsdfds")

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        dispatcher.dispatch(DataStoreAction.Reset)

        Mockito.verify(support.storage).reset()

        listObserver.assertLastValue(emptyList())
        stateObserver.assertLastValue(DataStore.State.Unprepared)
    }

    @Test
    fun testUserReset() {
        val stateObserver = createTestObserver<State>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        support.storage.unlock("fdsfsdfds")

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        dispatcher.dispatch(LifecycleAction.UserReset)

        Mockito.verify(support.storage).reset()

        listObserver.assertLastValue(emptyList())
        stateObserver.assertLastValue(DataStore.State.Unprepared)
    }

    @Test
    fun testResetSupport() {
        val stateObserver = createTestObserver<State>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)

        val newSupport = MockDataStoreSupport()
        Assert.assertNotSame("Support should be not the new one", newSupport, subject.support)

        stateObserver.assertLastValue(State.Unprepared)
        subject.resetSupport(newSupport)
        Assert.assertSame("Support should be the new one", newSupport, subject.support)

        stateObserver.assertLastValue(State.Unprepared)

        subject.unlock()
        stateObserver.assertLastValue(State.Unlocked)
        Assertions.assertThrows(IllegalStateException::class.java) {
            subject.resetSupport(MockDataStoreSupport())
        }
    }
}

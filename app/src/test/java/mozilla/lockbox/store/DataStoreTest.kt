/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.store.DataStore.State
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mozilla.sync15.logins.ServerPassword

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
            Assert.assertEquals(State.Locked, it)
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
            assertValueCount(2)
            assertValueAt(0, State.Unlocked)
            assertValueAt(1, State.Locked)
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
        Mockito.verify(support.storage).sync(support.syncConfig)
        Mockito.verify(support.storage).list()
        Mockito.clearInvocations(support.storage)

        dispatcher.dispatch(DataStoreAction.Lock)
        Mockito.verify(support.storage).lock()
        Mockito.clearInvocations(support.storage)
    }
}

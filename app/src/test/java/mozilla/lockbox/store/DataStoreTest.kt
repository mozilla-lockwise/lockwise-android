/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.flux.Dispatcher
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.mozilla.sync15.logins.ServerPassword

class DataStoreTest {
    private val dispatcher = Dispatcher()
    private val disposer = CompositeDisposable()

    @After
    fun tearDown() {
        disposer.clear()
    }

    @Test
    fun testInitialState() {
        val store = DataStore(dispatcher)
        Assert.assertSame(dispatcher, store.dispatcher)
        store.list.subscribe {
            Assert.assertTrue(it.isEmpty())
        }.addTo(disposer)
        store.state.subscribe {
            Assert.assertEquals(DataStoreState.Status.LOCKED, it.status)
            Assert.assertNull(it.error)
        }.addTo(disposer)
    }

    @Test
    fun testLockUnlock() {
        val stateObserver = createTestObserver<DataStoreState>()
        val listObserver = createTestObserver<List<ServerPassword>>()

        // TODO: mock backend for testing ...
        val store = DataStore(dispatcher)
        store.state.subscribe(stateObserver)
        store.list.subscribe(listObserver)

        stateObserver.values()
        var waiter = createTestObserver<Unit>()
        store.unlock().subscribe(waiter)
        waiter.await()
                .assertComplete()

        waiter = createTestObserver<Unit>()
        store.lock().subscribe(waiter)
        waiter.await()
                .assertComplete()

        stateObserver.apply {
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

    private fun <T> createTestObserver(): TestObserver<T> {
        val result = TestObserver.create<T>()
        result.addTo(disposer)
        return result
    }
}

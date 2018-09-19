/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.flux.Dispatcher
import org.junit.After
import org.junit.Assert
import org.junit.Test

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
        store.list.subscribe { items ->
            Assert.assertTrue(items.isEmpty())
        }.addTo(disposer)
        store.state.subscribe { state ->
            Assert.assertEquals(DataStoreState.Status.LOCKED, state.status)
            Assert.assertNull(state.error)
        }
    }
}
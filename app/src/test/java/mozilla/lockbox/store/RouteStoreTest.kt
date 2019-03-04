/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.support.AutoLockSupport
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class RouteStoreTest {
    class FakeLifecycleStore : LifecycleStore() {
        override val lifecycleEvents: Observable<LifecycleAction> = PublishSubject.create()
    }

    class FakeAutolockSupport : AutoLockSupport() {
        var shouldLockStub: Boolean = false
        override val shouldLock
            get() = shouldLockStub

        override fun storeNextAutoLockTime() {
        }

        override fun backdateNextLockTime() {
        }

        override fun forwardDateNextLockTime() {
        }
    }

    private val support = MockDataStoreSupport()

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val routeObserver = TestObserver.create<RouteAction>()
    private val dataStoreObserver = TestObserver.create<DataStore.State>()
    private val autoLockSupport = Mockito.spy(FakeAutolockSupport())
    private val lifecycleStore = FakeLifecycleStore()

    private val dataStore = DataStore(dispatcher, support, autoLockSupport, lifecycleStore)
    private val errorDataStore = DataStore(dispatcher)

    lateinit var subject: RouteStore

    private fun setUpSubject() {
        subject = RouteStore(dispatcher, dataStore)
        setupObservers()
    }

    private fun setUpErrorSubject() {
        subject = RouteStore(dispatcher, errorDataStore)
        setupObservers()
    }

    private fun setupObservers() {
        dataStore.state.subscribe(dataStoreObserver)
        subject.routes.subscribe(routeObserver)
        dispatcher.register.subscribe(dispatcherObserver)
    }

    @Test
    fun `dispatched routeactions`() {
        setUpSubject()
        val action = RouteAction.Welcome
        dispatcher.dispatch(action)

        routeObserver.assertValue(action)
    }

    @Test
    fun `dispatched non-routeactions`() {
        setUpSubject()
        val action = LifecycleAction.UserReset
        dispatcher.dispatch(action)

        routeObserver.assertEmpty()
    }

    @Test
    fun `datastore failed to create backend`() {
        setUpErrorSubject()

        dispatcher.dispatch(DataStoreAction.Sync)
        dispatcher.dispatch(OnboardingStatusAction(false))

        routeObserver.assertEmpty()
    }

    @Test
    fun `dispatched on datastore`() {
        setUpSubject()
        val routesIterator = subject.routes.blockingIterable().iterator()

        dispatcher.dispatch(OnboardingStatusAction(false))

        dispatcher.dispatch(DataStoreAction.Reset)
        Assert.assertEquals(routesIterator.next(), RouteAction.Welcome)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(routesIterator.next(), RouteAction.ItemList)

        dispatcher.dispatch(DataStoreAction.Lock)
        Assert.assertEquals(routesIterator.next(), RouteAction.LockScreen)
    }
}
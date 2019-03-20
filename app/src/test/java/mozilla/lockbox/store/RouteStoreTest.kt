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
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class RouteStoreTest {

    class FakeDataStore : DataStore() {
        val stateStub = PublishSubject.create<DataStore.State>()

        override val state: Observable<State>
            get() = stateStub
    }

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val routeObserver = TestObserver.create<RouteAction>()
    private val dataStore = FakeDataStore()

    lateinit var subject: RouteStore

    @Before
    fun setUp() {
        subject = RouteStore(dispatcher, dataStore)

        subject.routes.subscribe(routeObserver)
        dispatcher.register.subscribe(dispatcherObserver)
    }

    @Test
    fun `dispatched routeactions`() {
        val action = RouteAction.Welcome
        dispatcher.dispatch(action)

        routeObserver.assertValue(action)
    }

    @Test
    fun `dispatched route actions with no follow actions`() {
        dispatcher.dispatch(RouteAction.ItemList)
        routeObserver.assertLastValue(RouteAction.ItemList)

        dispatcher.dispatch(RouteAction.SystemSetting(SettingIntent.Security))
        val routeObserver1 = TestObserver.create<RouteAction>()
        subject.routes.subscribe(routeObserver1)
        routeObserver1.assertLastValue(RouteAction.ItemList)

        dispatcher.dispatch(RouteAction.OpenWebsite("https://mozilla.org"))
        val routeObserver2 = TestObserver.create<RouteAction>()
        subject.routes.subscribe(routeObserver2)
        routeObserver2.assertLastValue(RouteAction.ItemList)
    }

    @Test
    fun `dispatched non-routeactions`() {
        val action = LifecycleAction.UserReset
        dispatcher.dispatch(action)

        routeObserver.assertEmpty()
    }

    @Test
    fun `dispatched on datastore`() {
        dispatcher.dispatch(OnboardingStatusAction(false))

        dataStore.stateStub.onNext(DataStore.State.Errored(Exception("Fake exception for testing purpose")))
        routeObserver.assertEmpty()

        dataStore.stateStub.onNext(DataStore.State.Unprepared)
        routeObserver.assertLastValue(RouteAction.Welcome)

        dataStore.stateStub.onNext(DataStore.State.Unlocked)
        routeObserver.assertLastValue(RouteAction.ItemList)

        dataStore.stateStub.onNext(DataStore.State.Locked)
        routeObserver.assertLastValue(RouteAction.LockScreen)
    }
}
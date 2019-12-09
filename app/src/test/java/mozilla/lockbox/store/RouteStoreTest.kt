/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.LoginsStorageException
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@ExperimentalCoroutinesApi
class RouteStoreTest {
    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!
    private val stateStub = PublishSubject.create<DataStore.State>()

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val routeObserver = TestObserver.create<RouteAction>()

    lateinit var subject: RouteStore

    @Before
    fun setUp() {
        whenCalled(dataStore.state).thenReturn(stateStub)
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments().thenReturn(dataStore)

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

        stateStub.onNext(DataStore.State.Errored(LoginsStorageException("Fake exception for testing purpose")))
        routeObserver.assertEmpty()

        stateStub.onNext(DataStore.State.Unprepared)
        routeObserver.assertLastValue(RouteAction.Welcome)

        dispatcher.dispatch(RouteAction.LockScreen)
        stateStub.onNext(DataStore.State.Unlocked)
        routeObserver.assertLastValue(RouteAction.ItemList)

        stateStub.onNext(DataStore.State.Locked)
        routeObserver.assertLastValue(RouteAction.LockScreen)
    }

    @Test
    fun `unlock datastore actions only unlock when showing the lock screen`() {
        // These tests show that the datastore is locked when the app is backgrounded,
        // and unlocked when they re-foreground. However, they shouldn't cause the app to go back
        // to the default screen (ItemList), but stay where they are.

        // changes from lock screen to item list.
        dispatcher.dispatch(RouteAction.LockScreen)
        stateStub.onNext(DataStore.State.Unlocked)
        routeObserver.assertLastValue(RouteAction.ItemList)

        // no changes when the datastore is unlocked again.
        dispatcher.dispatch(RouteAction.Filter)
        stateStub.onNext(DataStore.State.Unlocked)
        routeObserver.assertLastValue(RouteAction.Filter)

        dispatcher.dispatch(RouteAction.DisplayItem(""))
        stateStub.onNext(DataStore.State.Unlocked)
        routeObserver.assertLastValue(RouteAction.DisplayItem(""))

        dispatcher.dispatch(RouteAction.AccountSetting)
        stateStub.onNext(DataStore.State.Unlocked)
        routeObserver.assertLastValue(RouteAction.AccountSetting)
    }
}

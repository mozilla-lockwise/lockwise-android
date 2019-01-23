/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.mockito.Mockito.`when` as whenCalled

@ExperimentalCoroutinesApi
class RouteStoreTest {

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val actionStub = PublishSubject.create<Action>()
    private val routeObserver = TestObserver.create<RouteAction>()
    private val onboardingObserver = TestObserver.create<Boolean>()

    lateinit var subject: RouteStore

    @Before
    fun setUp() {
        subject = RouteStore(dispatcher)

        subject.routes.subscribe(routeObserver)
        subject.onboarding.subscribe(onboardingObserver)

        dispatcher.register.subscribe { dispatcherObserver }
    }

    @Test
    fun `dispatched routeactions`() {
        val action = RouteAction.Welcome
        dispatcher.dispatch(action)

        routeObserver.assertValue(action)
    }

    @Test
    fun `dispatched non-routeactions`() {
        val action = LifecycleAction.UserReset
        dispatcher.dispatch(action)

        routeObserver.assertEmpty()
    }

    @Test
    fun `trigger onboarding when first time login`() {
        val action = RouteAction.Onboarding
        dispatcher.dispatch(action)

        dispatcherObserver.assertLastValue(RouteAction.Onboarding)
//        onboardingObserver.assertValue(true)
    }
}
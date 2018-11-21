/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when` as whenCalled

class RouteStoreTest {
    @Mock
    val dispatcher = Mockito.mock(Dispatcher::class.java)

    private val actionStub = PublishSubject.create<Action>()
    private val routeObserver = TestObserver.create<RouteAction>()

    lateinit var subject: RouteStore

    @Before
    fun setUp() {
        whenCalled(dispatcher.register).thenReturn(actionStub)

        subject = RouteStore(dispatcher)

        subject.routes.subscribe(routeObserver)
    }

    @Test
    fun `dispatched routeactions`() {
        val action = RouteAction.Welcome
        actionStub.onNext(action)

        routeObserver.assertValue(action)
    }

    @Test
    fun `dispatched non-routeactions`() {
        val action = LifecycleAction.UserReset
        actionStub.onNext(action)

        routeObserver.assertEmpty()
    }
}
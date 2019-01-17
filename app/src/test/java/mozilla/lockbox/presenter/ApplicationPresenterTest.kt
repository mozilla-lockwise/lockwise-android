/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class MockLifecycle(
    var state: Lifecycle.State
) : Lifecycle() {
    override fun getCurrentState() = state

    override fun addObserver(observer: LifecycleObserver) {
    }
    override fun removeObserver(observer: LifecycleObserver) {
    }
}

class ApplicationPresenterTest : DisposingTest() {
    private val dispatcher = Dispatcher()
    private val dispatcherObserver = createTestObserver<Action>()
    private val subject = ApplicationPresenter(dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
    }

    @Test
    fun testOnCreate() {
        subject.onCreate(createLifecycleOwner(Lifecycle.State.CREATED))
        dispatcherObserver.assertValue(LifecycleAction.Startup)
    }

    @Test
    fun testBackgrouding() {
        subject.onPause(createLifecycleOwner(Lifecycle.State.STARTED))
        dispatcherObserver.assertValue(LifecycleAction.Background)
    }

    @Test
    fun testForegrounding() {
        subject.onResume(createLifecycleOwner(Lifecycle.State.RESUMED))
        dispatcherObserver.assertValue(LifecycleAction.Foreground)
    }

    private fun createLifecycleOwner(state: Lifecycle.State) = object : LifecycleOwner {
        override fun getLifecycle() = MockLifecycle(state)
    }
}
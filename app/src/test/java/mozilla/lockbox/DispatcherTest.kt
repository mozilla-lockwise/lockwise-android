/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

private enum class TestAction : Action {
    UNIT
}

class DispatcherTest {

    @Test
    fun testSingleObserver() {
        val dispatcher = Dispatcher()
        val onNextExecuted = AtomicBoolean(false)

        val subscription = dispatcher.register.subscribe { onNextExecuted.set(true) }
        assertFalse(onNextExecuted.get())
        dispatcher.dispatch(TestAction.UNIT)
        assertTrue(onNextExecuted.get())

        subscription.dispose()
    }

    @Test
    fun testMultipleObserver() {
        val dispatcher = Dispatcher()
        val num = 10

        val executedFlags = Array(num, { _ -> AtomicBoolean(false) })
        val subscriptions = executedFlags.map { f -> dispatcher.register.subscribe { _ -> f.set(true) } }

        executedFlags.forEach { b -> assertFalse(b.get()) }
        dispatcher.dispatch(TestAction.UNIT)

        executedFlags.forEach { b -> assertTrue(b.get()) }
        subscriptions.forEach { s -> s.dispose() }
    }
}

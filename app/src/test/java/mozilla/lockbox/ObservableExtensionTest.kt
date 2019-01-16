/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import io.reactivex.subjects.PublishSubject
import mozilla.components.support.base.log.logger.Logger
import mozilla.lockbox.extensions.debug
import mozilla.lockbox.extensions.filterByType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import java.util.concurrent.atomic.AtomicBoolean

private enum class ValidAction {
    VALID
}

private enum class InvalidAction {
    INVALID
}

@RunWith(PowerMockRunner::class)
@PrepareForTest(LogProvider::class)
class ObservableExtensionTest {
    @Mock
    val logger = Mockito.mock(Logger::class.java)

    @Before
    fun setUp() {
        PowerMockito.mockStatic(LogProvider::class.java)
        Whitebox.setInternalState(LogProvider::class.java, "log", logger)
    }

    @Test
    fun testFilterByType() {
        val subject = PublishSubject.create<Any>()
        var flag = AtomicBoolean(false)
        val subscription = subject.filterByType(ValidAction::class.java).subscribe { _ -> flag.set(true) }

        subject.onNext(InvalidAction.INVALID)
        assertFalse(flag.get())

        subject.onNext(ValidAction.VALID)
        assertTrue(flag.get())

        subscription.dispose()
    }

    @Test
    fun debug() {
        val subject = PublishSubject.create<String>()
        val observer = "OBSERVER"
        subject.debug(observer).subscribe({}, {})

        verify(logger).info("$observer: subscribed")

        val eventMessage = "message"
        subject.onNext(eventMessage)

        verify(logger).info("$observer: event: $eventMessage")

        val errorMessage = "error_message"
        val error = Throwable(message = errorMessage)
        subject.onError(error)

        verify(logger).info("$observer: error: $errorMessage")
    }
}
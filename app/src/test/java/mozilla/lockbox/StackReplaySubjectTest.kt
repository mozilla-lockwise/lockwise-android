/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import io.reactivex.observers.TestObserver
import junit.framework.Assert.assertEquals
import mozilla.lockbox.extensions.assertLastValue

import mozilla.lockbox.flux.StackReplaySubject
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class StackReplaySubjectTest {

    private val subject = StackReplaySubject.create<Int>()

    private val testObserver = TestObserver<Int>()

    @Test
    fun testSimpleObserving() {
        subject.subscribe(testObserver)

        subject.onNext(0)
        testObserver.assertLastValue(0)

        subject.onNext(1)
        testObserver.assertLastValue(1)
    }

    @Test
    fun testingSubscriptionDisposal() {
        val flag = AtomicInteger(0)
        val disposable1 = subject.subscribe {
            flag.incrementAndGet()
        }

        flag.set(0)
        subject.onNext(0)
        assertEquals(1, flag.get())

        val disposable2 = subject.subscribe {
            flag.incrementAndGet()
        }

        flag.set(0)
        subject.onNext(0)
        assertEquals(2, flag.get())

        flag.set(0)
        disposable1.dispose()
        subject.onNext(0)
        assertEquals(1, flag.get())

        flag.set(0)
        disposable2.dispose()

        subject.onNext(0)
        assertEquals(0, flag.get())
    }

    @Test
    fun testReplay() {
        subject.onNext(1)
        subject.onNext(2)
        assertEquals(2, subject.getSize())

        subject.subscribe(testObserver)
        testObserver.assertLastValue(2)

        subject.onNext(3)
        assertEquals(3, subject.getSize())
        testObserver.assertLastValue(3)
    }

    @Test
    fun testReplayOnEmpty() {
        assertEquals(0, subject.getSize())
        subject.subscribe(testObserver)
        testObserver.assertEmpty()
    }

    @Test
    fun testPop() {
        subject.onNext(1)
        subject.onNext(2)
        assertEquals(2, subject.getSize())

        subject.pop()
        assertEquals(1, subject.getSize())
        subject.subscribe(testObserver)
        testObserver.assertLastValue(1)

        // pop isn't observed.
        subject.onNext(3)
        assertEquals(2, subject.getSize())
        testObserver.assertLastValue(3)
        subject.pop()
        assertEquals(1, subject.getSize())
        testObserver.assertLastValue(3)
    }

    @Test
    fun testTrim() {
        subject.onNext(1)
        subject.onNext(2)
        subject.onNext(3)
        assertEquals(3, subject.getSize())

        subject.trim(2, 3)
        assertEquals(1, subject.getSize())
        subject.subscribe(testObserver)
        testObserver.assertLastValue(1)
    }

    @Test
    fun testClear() {
        subject.onNext(1)
        subject.onNext(2)
        subject.onNext(3)
        assertEquals(3, subject.getSize())
        subject.clear()
        assertEquals(0, subject.getSize())

        subject.subscribe(testObserver)
        testObserver.assertEmpty()
    }

    @Test
    fun testTrimTail() {
        subject.onNext(1)
        subject.onNext(2)
        subject.onNext(3)
        assertEquals(3, subject.getSize())

        subject.trimTail()
        assertEquals(1, subject.getSize())
        subject.subscribe(testObserver)
        testObserver.assertLastValue(3)
    }
}

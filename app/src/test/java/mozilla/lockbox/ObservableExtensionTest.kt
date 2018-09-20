/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.extensions.filterByType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

private enum class ValidAction {
    VALID
}

private enum class InvalidAction {
    INVALID
}

class ObservableExtensionTest {

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
}

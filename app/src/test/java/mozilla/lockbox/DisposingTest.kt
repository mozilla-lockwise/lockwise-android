/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.addTo
import org.junit.After

open class DisposingTest {
    val disposer = CompositeDisposable()

    @After
    open fun tearDown() {
        disposer.clear()
    }

    fun <T> createTestObserver(): TestObserver<T> {
        val result = TestObserver.create<T>()
        result.addTo(disposer)
        return result
    }
}
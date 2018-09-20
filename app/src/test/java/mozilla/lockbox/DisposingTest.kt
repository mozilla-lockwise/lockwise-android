/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import io.reactivex.disposables.CompositeDisposable
import org.junit.After

open class DisposingTest {
    val disposer = CompositeDisposable()

    @After
    open fun tearDown() {
        disposer.clear()
    }
}
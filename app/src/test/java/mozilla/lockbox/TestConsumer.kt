/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import io.reactivex.Observer
import io.reactivex.functions.Consumer

class TestConsumer<T>(private val observer: Observer<T>) : Consumer<T> {
    override fun accept(t: T) {
        observer.onNext(t)
    }
}
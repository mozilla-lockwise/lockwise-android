/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import io.reactivex.observers.TestObserver

fun <T> TestObserver<T>.assertLastValue(expectedValue: T): TestObserver<T> {
    val count = this.valueCount()
    this.assertValueAt(count - 1, expectedValue)
    return this
}

fun <T> TestObserver<T>.assertLastValueMatches(expectedPredicate: (value: T) -> Boolean): TestObserver<T> {
    val count = this.valueCount()
    this.assertValueAt(count - 1, expectedPredicate)
    return this
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

class Consumable<T>(private val value: T) {
    private var consumed = false

    fun get(): T? {
        return if (consumed) {
            null
        } else {
            consumed = true
            value
        }
    }
}
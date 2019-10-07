/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log

private val dispatcher: Dispatcher = Dispatcher.shared

fun pushError(throwable: Throwable, message: String? = null) {
    log.error(message)
    dispatcher.dispatch(SentryAction(throwable))
}

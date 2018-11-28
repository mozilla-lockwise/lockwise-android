/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import mozilla.lockbox.BuildConfig

fun isDebug(): Boolean {
    return BuildConfig.DEBUG
}

fun assertOnUiThread(detailMessage: String = "Should be on the UI thread") {
    if (isDebug() && !isOnUiThread()) {
        throw AssertionError(detailMessage)
    }
}

fun assertNotOnUiThread(detailMessage: String = "Should not be on the UI thread") {
    if (isDebug() && isOnUiThread()) {
        throw AssertionError(detailMessage)
    }
}

fun isTesting(): Boolean {
    try {
        Class.forName("org.robolectric.RuntimeEnvironment")
    } catch (e: ClassNotFoundException) {
        return false
    }
    return true
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import android.os.Looper

fun isOnUiThread(): Boolean {
    return Looper.getMainLooper() == Looper.myLooper()
}

fun dpToPixels(context: Context, dp: Float): Int {
    val metrics = context.resources.displayMetrics
    val fpixels = metrics.density * dp
    return (fpixels + 0.5f).toInt()
}
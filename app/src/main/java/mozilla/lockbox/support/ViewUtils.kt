/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.view.View

fun showView(vararg views: View) {
    for (view in views) {
        view.visibility = View.VISIBLE
    }
}

fun removeView(view: View) {
    view.visibility = View.GONE
}

fun <S : View, R : View?> showAndRemove(toShow: S, vararg toRemove: R) {
    showView(toShow)
    for (view in toRemove) {
        if (view != null) {
            removeView(view)
        }
    }
}
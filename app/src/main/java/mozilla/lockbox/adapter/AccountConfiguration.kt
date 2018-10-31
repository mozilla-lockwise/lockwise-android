/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.graphics.drawable.ShapeDrawable

class AccountConfiguration(
    val email: String,
    val summary: String? = null,
    val buttonTitle: String,
    val buttonSummary: String? = null)

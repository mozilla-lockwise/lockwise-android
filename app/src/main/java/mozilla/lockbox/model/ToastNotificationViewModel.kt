/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ToastNotificationViewModel(
    @StringRes val message: Int,
    @DrawableRes val icon: Int,
    val messageParam: String? = null
)
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ToastNotificationViewModel(
    val text: String? = null,
    @StringRes val strId: Int? = null,
    @DrawableRes val img: Int
)
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

import androidx.annotation.StringRes

data class DialogViewModel(
    @StringRes val title: Int? = null,
    @StringRes val message: Int? = null,
    @StringRes val positiveButtonTitle: Int? = null,
    @StringRes val negativeButtonTitle: Int? = null,
    val isDestructive: Boolean = false
)
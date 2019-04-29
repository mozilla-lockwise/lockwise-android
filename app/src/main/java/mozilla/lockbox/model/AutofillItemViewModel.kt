/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.model

data class AutofillItemViewModel(
    val autofillId: String,
    val hostName: String,
    val username: String?,
    val password: String?
)
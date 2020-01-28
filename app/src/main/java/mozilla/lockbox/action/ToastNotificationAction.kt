/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.R
import mozilla.lockbox.model.ToastNotificationViewModel

sealed class ToastNotificationAction(
    val viewModel: ToastNotificationViewModel
) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.toast) {

    object ShowCopyUsernameToast : ToastNotificationAction(
        ToastNotificationViewModel(
            R.string.toast_username_copied,
            R.drawable.ic_check
        )
    )

    object ShowCopyPasswordToast : ToastNotificationAction(
        ToastNotificationViewModel(
            R.string.toast_password_copied,
            R.drawable.ic_check
        )
    )

    object ShowSuccessfulCreateToast : ToastNotificationAction(
        ToastNotificationViewModel(
            R.string.successful_create_toast,
            R.drawable.ic_success
        )
    )

    data class ShowDeleteToast(val entryName: String?) : ToastNotificationAction(
        ToastNotificationViewModel(
            R.string.entry_deleted_toast,
            R.drawable.ic_delete_red,
            entryName
        )
    )
}

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
            strId = R.string.toast_username_copied
        )
    )

    object ShowCopyPasswordToast : ToastNotificationAction(
        ToastNotificationViewModel(
            strId = R.string.toast_password_copied
        )
    )

    object ShowSuccessfulCreateToast : ToastNotificationAction(
        ToastNotificationViewModel(
            strId = R.string.successful_create_toast
        )
    )

    object ShowAutofillErrorToast : ToastNotificationAction(
        ToastNotificationViewModel(
            strId = R.string.autofill_error_toast
        )
    )

    data class ShowDeleteToast(val text: String?) : ToastNotificationAction(
        ToastNotificationViewModel(
            text = text?.plus(" deleted.")
        )
    )
}

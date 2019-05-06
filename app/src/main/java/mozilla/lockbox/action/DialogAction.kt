/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.R
import mozilla.lockbox.flux.Action
import mozilla.lockbox.model.DialogViewModel

sealed class DialogAction(
    val viewModel: DialogViewModel,
    val positiveButtonActionList: List<Action> = emptyList(),
    val negativeButtonActionList: List<Action> = emptyList()
) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.dialog) {
    object SecurityDisclaimer : DialogAction(
        DialogViewModel(
            R.string.no_device_security_title,
            R.string.no_device_security_message,
            R.string.set_up_security_button,
            R.string.cancel
        ),
        listOf(RouteAction.SystemSetting(SettingIntent.Security))
    )
    object UnlinkDisclaimer : DialogAction(
        DialogViewModel(
            R.string.disconnect_disclaimer_title,
            R.string.disconnect_disclaimer_message,
            R.string.disconnect,
            R.string.cancel,
            R.color.red
        ),
        listOf(LifecycleAction.UserReset)
    )
    object OnboardingSecurityDialog : DialogAction(
            DialogViewModel(
                R.string.secure_your_device,
                R.string.device_security_description,
                R.string.set_up_now,
                R.string.skip_button
            ),
            listOf(
                RouteAction.SystemSetting(SettingIntent.Security),
                RouteAction.Login
            ),
            listOf(RouteAction.Login)
        )
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.appservices.logins.ServerPassword
import mozilla.components.service.fxa.sharing.ShareableAccount
import mozilla.lockbox.R
import mozilla.lockbox.flux.Action
import mozilla.lockbox.model.DialogViewModel
import mozilla.lockbox.model.titleFromHostname

/**
 * Dispatching a `DialogAction` causes a modal dialog to be displayed.
 *
 * On the user pressing either a the positive or negative buttons a list of actions
 * are dispatched.
 *
 * Hint: DialogActions are not added to history stack. The edge linking where the dialog is shown to where
 * then buttons should take the user should be represented in the `RoutePresenter` and the `graph.xml`.
 */
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
        listOf(SystemSetting(SettingIntent.Security))
    )

    object UnlinkDisclaimer : DialogAction(
        DialogViewModel(
            R.string.disconnect_disclaimer_title,
            R.string.disconnect_disclaimer_message,
            R.string.disconnect,
            R.string.cancel,
            true
        ),
        listOf(LifecycleAction.UserReset)
    )

    data class OnboardingSecurityDialogAutomatic(val account: ShareableAccount) : DialogAction(
        DialogViewModel(
            R.string.secure_your_device,
            R.string.device_security_description,
            R.string.set_up_now,
            R.string.skip_button
        ),
        listOf(
            SystemSetting(SettingIntent.Security),
            AccountAction.AutomaticLogin(account)
        ),
        listOf(Login)
    )

    data class DeleteConfirmationDialog(
        val item: ServerPassword
    ) : DialogAction(
        DialogViewModel(
            R.string.delete_this_login,
            R.string.delete_description,
            R.string.delete,
            R.string.cancel,
            true
        ),
        listOf(
            DataStoreAction.Delete(item),
            ItemList,
            ToastNotificationAction.ShowDeleteToast(titleFromHostname(item.hostname))
        )
    )

    object OnboardingSecurityDialogManual : DialogAction(
            DialogViewModel(
                R.string.secure_your_device,
                R.string.device_security_description,
                R.string.set_up_now,
                R.string.skip_button
            ),
            listOf(
                SystemSetting(SettingIntent.Security),
                Login
            ),
            listOf(Login)
    )

    data class DiscardChangesDialog(
        val itemId: String
    ) : DialogAction(
        DialogViewModel(
            R.string.discard_changes,
            R.string.discard_changes_description,
            R.string.discard,
            R.string.cancel,
            true
        ),
        listOf(
            ItemDetailAction.EndEditItemSession
        )
    )

    object DiscardChangesCreateDialog : DialogAction(
        DialogViewModel(
            R.string.discard_changes,
            R.string.discard_changes_description,
            R.string.discard,
            R.string.cancel,
            true
        ),
        listOf(
            DiscardCreateItemNoChanges
        )
    )
}

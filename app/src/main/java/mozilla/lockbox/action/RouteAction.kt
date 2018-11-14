/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import android.support.annotation.StringRes
import mozilla.lockbox.flux.Action

sealed class RouteAction : Action {
    object Welcome : RouteAction()
    object ItemList : RouteAction()
    object Login : RouteAction()
    object SettingList : RouteAction()
    object AccountSetting : RouteAction()
    object Back : RouteAction()
    object LockScreen : RouteAction()
    object Filter : RouteAction()
    data class ItemDetail(val id: String) : RouteAction()
    data class OpenWebsite(val url: String) : RouteAction()
    data class SystemSetting(val setting: SettingIntent) : RouteAction()

    sealed class Dialog(
        val positiveButtonActions: Action? = null,
        val negativeButtonActions: Action? = null
    ) : RouteAction() {
        object SecurityDisclaimer : Dialog(RouteAction.SystemSetting(SettingIntent.Security))
        object UnlinkDisclaimer : Dialog(LifecycleAction.UserReset)
    }

    sealed class DialogFragment(@StringRes val dialogTitle: Int, @StringRes val dialogSubtitle: Int? = null) : RouteAction() {
        class FingerprintDialog(@StringRes title: Int, @StringRes subtitle: Int? = null) :
            DialogFragment(dialogTitle = title, dialogSubtitle = subtitle)
    }
}

enum class SettingIntent(val intentAction: String) {
    Security(android.provider.Settings.ACTION_SECURITY_SETTINGS)
}

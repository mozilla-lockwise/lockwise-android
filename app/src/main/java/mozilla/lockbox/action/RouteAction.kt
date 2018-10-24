/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action

sealed class RouteAction : Action {
    object Welcome : RouteAction()
    object ItemList : RouteAction()
    object Login : RouteAction()
    object SettingList : RouteAction()
    object Back : RouteAction()
    object LockScreen : RouteAction()
    object Filter : RouteAction()
    object FingerprintDialog : RouteAction()
    data class ItemDetail(val id: String) : RouteAction()
    data class OpenWebsite(val url: String) : RouteAction()
    data class SystemSetting(val setting: SettingIntent) : RouteAction()

    sealed class DialogAction(
        val positiveButtonAction: Action? = null,
        val negativeButtonAction: Action? = null
    ) : RouteAction() {
        class SecurityDisclaimerDialog(setUpAction: Action) : DialogAction(positiveButtonAction = setUpAction)
    }
}

enum class SettingIntent(val settingIntent: String) {
    Security(android.provider.Settings.ACTION_SECURITY_SETTINGS)
}

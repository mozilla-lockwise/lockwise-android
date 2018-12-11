/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import android.support.annotation.StringRes
import mozilla.lockbox.flux.Action

sealed class RouteAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject
) : TelemetryAction {
    object Welcome : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.login_welcome)
    object ItemList : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.entry_list)
    object Login : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.login_fxa)
    object SettingList : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.settings_list)
    object AccountSetting : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.settings_account)
    object AutoLockSetting : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.settings_autolock)
    object Back : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.back)
    object LockScreen : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.lock_screen)
    object Filter : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.filter)
    data class ItemDetail(val id: String) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.entry_detail)
    data class OpenWebsite(val url: String) : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.open_in_browser)
    data class SystemSetting(val setting: SettingIntent) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.settings_system)

    sealed class Dialog(
        val positiveButtonAction: Action? = null,
        val negativeButtonAction: Action? = null
    ) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.dialog) {
        object SecurityDisclaimer : Dialog(RouteAction.SystemSetting(SettingIntent.Security))
        object UnlinkDisclaimer : Dialog(LifecycleAction.UserReset)
    }

    sealed class DialogFragment(@StringRes val dialogTitle: Int, @StringRes val dialogSubtitle: Int? = null) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.dialog) {
        class FingerprintDialog(@StringRes title: Int, @StringRes subtitle: Int? = null) :
            DialogFragment(dialogTitle = title, dialogSubtitle = subtitle)
    }
}

enum class SettingIntent(val intentAction: String) {
    Security(android.provider.Settings.ACTION_SECURITY_SETTINGS)
}

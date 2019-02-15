/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import mozilla.lockbox.R
import mozilla.lockbox.flux.Action
import mozilla.lockbox.support.Constant

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
    object LockScreen : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.lock_screen)
    object Filter : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.filter)
    data class ItemDetail(val id: String) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.entry_detail)
    data class OpenWebsite(val url: String) :
        RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.open_in_browser)

    data class SystemSetting(val setting: SettingIntent) :
        RouteAction(TelemetryEventMethod.show, TelemetryEventObject.settings_system)

    sealed class Dialog(
        val positiveButtonAction: Action? = null,
        val negativeButtonAction: Action? = null
    ) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.dialog) {
        object SecurityDisclaimer : Dialog(RouteAction.SystemSetting(SettingIntent.Security))
        object UnlinkDisclaimer : Dialog(LifecycleAction.UserReset)
        object NoNetworkDisclaimer : Dialog()
    }

    sealed class DialogFragment(
        @StringRes val dialogTitle: Int,
        @StringRes val dialogSubtitle: Int? = null
    ) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.dialog) {
        class FingerprintDialog(@StringRes title: Int, @StringRes subtitle: Int? = null) :
            DialogFragment(dialogTitle = title, dialogSubtitle = subtitle)
        class OnboardingSecurityDialog(@StringRes title: Int, @StringRes subtitle: Int? = null) :
            DialogFragment(dialogTitle = title, dialogSubtitle = subtitle)
    }

    sealed class AppWebPage(
        val url: String? = null,
        @StringRes val title: Int? = null,
        eventObject: TelemetryEventObject
    ) : RouteAction(TelemetryEventMethod.show, eventObject) {

        object FaqList : AppWebPage(
            Constant.Faq.topUri,
            R.string.nav_menu_faq,
            TelemetryEventObject.settings_faq
        )

        object FaqWelcome : AppWebPage(
            Constant.Faq.savedUri,
            R.string.nav_menu_faq,
            TelemetryEventObject.settings_faq)

        object FaqSecurity : AppWebPage(
            Constant.Faq.securityUri,
            R.string.nav_menu_faq,
            TelemetryEventObject.settings_faq)

        object FaqSync : AppWebPage(
            Constant.Faq.syncUri,
            R.string.nav_menu_faq,
            TelemetryEventObject.settings_faq)

        object FaqCreate : AppWebPage(
            Constant.Faq.createUri,
            R.string.nav_menu_faq,
            TelemetryEventObject.settings_faq)

        object FaqEdit : AppWebPage(
            Constant.Faq.editUri,
            R.string.nav_menu_faq,
            TelemetryEventObject.settings_faq)

        object Privacy : AppWebPage(
            Constant.Privacy.uri,
            R.string.privacy,
            TelemetryEventObject.settings_faq)

        object SendFeedback : AppWebPage(
            Constant.SendFeedback.uri,
            R.string.nav_menu_feedback,
            TelemetryEventObject.settings_provide_feedback
        )
    }

    sealed class Onboarding(
        eventObject: TelemetryEventObject
    ) : RouteAction(TelemetryEventMethod.show, eventObject) {
        object FingerprintAuth : Onboarding(TelemetryEventObject.onboarding_fingerprint)
        object Autofill : Onboarding(TelemetryEventObject.onboarding_autofill)
        object Confirmation : Onboarding(TelemetryEventObject.login_onboarding_confirmation)
    }
}

data class OnboardingStatusAction(val onboardingInProgress: Boolean) : Action

enum class SettingIntent(val intentAction: String, val data: Uri? = null) {
    Security(android.provider.Settings.ACTION_SECURITY_SETTINGS),
    @RequiresApi(Build.VERSION_CODES.O)
    Autofill(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE, Uri.parse("package:com.mozilla.lockbox"))
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import mozilla.lockbox.R
import mozilla.lockbox.flux.Action
import mozilla.lockbox.support.Constant

open class RouteAction(
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
    object CreateItem : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.begin_manual_create_session)
    object DiscardCreateItemNoChanges : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.discard_manual_create_no_changes)
    data class DisplayItem(val id: String) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.entry_detail)
    data class EditItem(val id: String) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.edit_entry_detail)

    data class ShowToastNotification(
        @StringRes val strId: Int? = null,
        val text: String? = null,
        val viewGroup: ViewGroup
    ) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.successful_save_toast)

    // This should _only_ be triggered by pressing the back button.
    object InternalBack : RouteAction(TelemetryEventMethod.tap, TelemetryEventObject.back)

    sealed class DialogFragment(
        @StringRes val dialogTitle: Int,
        @StringRes val dialogSubtitle: Int? = null
    ) : RouteAction(TelemetryEventMethod.show, TelemetryEventObject.dialog) {
        class FingerprintDialog(@StringRes title: Int, @StringRes subtitle: Int? = null) :
            DialogFragment(dialogTitle = title, dialogSubtitle = subtitle)

        object AutofillSearchDialog : DialogFragment(R.string.autofill)
    }

    open class SystemIntent(
        val requestCode: Int = Constant.RequestCode.noResult,
        eventMethod: TelemetryEventMethod,
        eventObject: TelemetryEventObject
    ) : RouteAction(eventMethod, eventObject)

    sealed class Onboarding(
        eventObject: TelemetryEventObject
    ) : RouteAction(TelemetryEventMethod.show, eventObject) {
        object FingerprintAuth : Onboarding(TelemetryEventObject.onboarding_fingerprint)
        object Autofill : Onboarding(TelemetryEventObject.onboarding_autofill)
        object Confirmation : Onboarding(TelemetryEventObject.login_onboarding_confirmation)
    }

    object UnlockFallbackDialog : SystemIntent(
        Constant.RequestCode.unlock,
        TelemetryEventMethod.show,
        TelemetryEventObject.dialog
    )

    data class OpenWebsite(val url: String) : SystemIntent(
        Constant.RequestCode.noResult,
        TelemetryEventMethod.tap,
        TelemetryEventObject.open_in_browser
    )

    data class SystemSetting(val setting: SettingIntent) : SystemIntent(
        Constant.RequestCode.noResult,
        TelemetryEventMethod.show,
        TelemetryEventObject.settings_system
    )
}

data class OnboardingStatusAction(val onboardingInProgress: Boolean) : Action

enum class SettingIntent(val intentAction: String, val data: Uri? = null) {
    Security(android.provider.Settings.ACTION_SECURITY_SETTINGS),
    @RequiresApi(Build.VERSION_CODES.O)
    Autofill(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE, Uri.parse("package:com.mozilla.lockbox"))
}

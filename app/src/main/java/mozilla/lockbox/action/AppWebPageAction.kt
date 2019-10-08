/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import androidx.annotation.StringRes
import mozilla.lockbox.R
import mozilla.lockbox.support.Constant

sealed class AppWebPageAction(
    val url: String? = null,
    @StringRes val title: Int? = null,
    eventObject: TelemetryEventObject
) : RouteAction(TelemetryEventMethod.show, eventObject) {

    object FaqList : AppWebPageAction(
        Constant.Faq.faqUri,
        R.string.nav_menu_faq,
        TelemetryEventObject.settings_faq)

    object FaqWelcome : AppWebPageAction(
        Constant.Faq.faqUri,
        R.string.nav_menu_faq,
        TelemetryEventObject.settings_faq)

    object FaqSync : AppWebPageAction(
        Constant.Faq.syncUri,
        R.string.nav_menu_faq,
        TelemetryEventObject.settings_faq)

    object Privacy : AppWebPageAction(
        Constant.Privacy.uri,
        R.string.privacy,
        TelemetryEventObject.settings_faq)

    object SendFeedback : AppWebPageAction(
        Constant.SendFeedback.uri,
        R.string.nav_menu_feedback,
        TelemetryEventObject.settings_provide_feedback)
}
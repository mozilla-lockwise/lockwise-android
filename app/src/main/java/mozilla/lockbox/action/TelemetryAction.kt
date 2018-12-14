/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.flux.Action
import org.mozilla.telemetry.event.TelemetryEvent

interface TelemetryAction : Action {
    val eventMethod: TelemetryEventMethod
    val eventObject: TelemetryEventObject
    val value: String?
        get() = null
    val extras: Map<String, Any>?
        get() = null

    open fun createEvent(category: String = "action"): TelemetryEvent {
        val evt = TelemetryEvent.create(
                category,
                eventMethod.name,
                eventObject.name,
                value
        )
        extras?.forEach { ex -> evt.extra(ex.key, ex.value.toString()) }

        return evt
    }
}

enum class TelemetryEventMethod {
    tap,
    startup,
    foreground,
    background,
    settingChanged,
    show,
    canceled,
    login_selected,
    autofill_locked,
    autofill_unlocked,
    refresh,
    autofill_clear
}

enum class TelemetryEventObject {
    app,
    entry_list,
    entry_detail,
    learn_more,
    reveal_password,
    entry_copy_username_button,
    entry_copy_password_button,
    settings_list,
    settings_autolock_time,
    settings_autolock,
    settings_reset,
    settings_preferred_browser,
    settings_record_usage_data,
    settings_account,
    settings_item_list_sort,
    settings_faq,
    settings_provide_feedback,
    settings_get_support,
    settings_system,
    login_welcome,
    login_fxa,
    login_onboarding_confirmation,
    login_learn_more,
    autofill_onboarding,
    autofill,
    lock_screen,
    open_in_browser,
    filter,
    back,
    dialog
}

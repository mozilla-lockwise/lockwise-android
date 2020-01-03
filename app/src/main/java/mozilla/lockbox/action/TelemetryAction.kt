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

    fun createEvent(category: String = "action"): TelemetryEvent {
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
    setting_changed,
    show,
    autofill_locked,
    lock,
    unlock,
    reset,
    sync_start,
    sync_end,
    sync_error,
    list_update,
    list_update_error,
    touch,
    update_credentials,
    delete,
    edit,
    autofill_single,
    autofill_multiple,
    autofill_cancel,
    autofill_error,
    autofill_filter,
    autofill_add,
    create,
    create_item_error
}

enum class TelemetryEventObject {
    app,
    entry_list,
    entry_detail,
    learn_more,
    reveal_password,
    entry_copy_username_button,
    entry_copy_password_button,
    onboarding_fingerprint,
    onboarding_autofill,
    settings_list,
    settings_autolock_time,
    settings_autolock,
    settings_reset,
    settings_record_usage_data,
    settings_account,
    settings_faq,
    settings_provide_feedback,
    settings_system,
    settings_fingerprint,
    settings_fingerprint_pending_auth,
    settings_item_list_order,
    settings_autofill,
    login_welcome,
    login_fxa,
    login_onboarding_confirmation,
    autofill,
    lock_screen,
    open_in_browser,
    filter,
    back,
    dialog,
    datastore,
    delete_credential,
    edit_entry_detail,
    update_credential,
    begin_edit_item_session,
    end_edit_item_session,
    begin_manual_create_session,
    end_manual_create_session,
    discard_manual_create_no_changes,
    manual_create_save,
    manual_create_datastore_save,
    successful_save_toast;
}

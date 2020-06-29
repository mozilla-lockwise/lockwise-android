/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import androidx.annotation.StringRes
import mozilla.lockbox.R

open class SettingAction(
    override val eventMethod: TelemetryEventMethod,
    override val eventObject: TelemetryEventObject,
    override val value: String?
) : TelemetryAction {

    data class UnlockWithFingerprint(val unlockWithFingerprint: Boolean)
        : SettingAction(
            TelemetryEventMethod.setting_changed,
            TelemetryEventObject.settings_fingerprint,
            unlockWithFingerprint.toString()
        )
    data class UnlockWithFingerprintPendingAuth(val unlockWithFingerprintPendingAuth: Boolean)
        : SettingAction(
            TelemetryEventMethod.setting_changed,
            TelemetryEventObject.settings_fingerprint_pending_auth,
            unlockWithFingerprintPendingAuth.toString()
        )
    data class SendUsageData(val sendUsageData: Boolean)
        : SettingAction(
            TelemetryEventMethod.setting_changed,
            TelemetryEventObject.settings_record_usage_data,
            null
        )
    data class ItemListSortOrder(val sortOrder: Setting.ItemListSort)
        : SettingAction(
            TelemetryEventMethod.setting_changed,
            TelemetryEventObject.settings_item_list_order,
            null
        )
    data class AutoLockTime(val time: Setting.AutoLockTime)
        : SettingAction(
            TelemetryEventMethod.setting_changed,
            TelemetryEventObject.settings_autolock_time,
            time.seconds.toString()
        )
    data class Autofill(val enable: Boolean)
        : SettingAction(
            TelemetryEventMethod.setting_changed,
            TelemetryEventObject.settings_autofill,
            null
        )
    object Reset : SettingAction(
        TelemetryEventMethod.setting_changed,
        TelemetryEventObject.settings_reset,
        null
    )
}

class Setting {
    enum class AutoLockTime(
        @StringRes val stringValue: Int,
        val seconds: Long
    ) {
        OneMinute(R.string.one_minute, 60),
        FiveMinutes(R.string.five_minutes, 60 * 5),
        FifteenMinutes(R.string.fifteen_minutes, 60 * 15),
        ThirtyMinutes(R.string.thirty_minutes, 60 * 30),
        OneHour(R.string.one_hour, 60 * 60),
        TwelveHours(R.string.twelve_hours, 60 * 60 * 12),
        TwentyFourHours(R.string.twenty_four_hours, 60 * 60 * 24),
        Never(R.string.never, 0);

        val ms: Long = this.seconds * 1000
    }

    enum class ItemListSort(@StringRes val titleId: Int, @StringRes val valueId: Int) {
        ALPHABETICALLY(R.string.all_logins_a_z, R.string.sort_menu_az),
        RECENTLY_USED(R.string.all_logins_recent, R.string.sort_menu_recent)
    }
}

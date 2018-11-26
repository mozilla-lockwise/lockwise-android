/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import android.support.annotation.StringRes
import mozilla.lockbox.R
import mozilla.lockbox.flux.Action

open class SettingAction : Action {
    data class UnlockWithFingerprint(val unlockWithFingerprint: Boolean) : SettingAction()
    data class UnlockWithFingerprintPendingAuth(val unlockWithFingerprintPendingAuth: Boolean) : SettingAction()
    data class SendUsageData(val sendUsageData: Boolean) : SettingAction()
    data class ItemListSortOrder(val sortOrder: Setting.ItemListSort) : SettingAction()
    data class AutoLockTime(val time: Setting.AutoLockTime) : SettingAction()
    object Reset : SettingAction()
}

class Setting {
    enum class AutoLockTime {
        OneMinute,
        FiveMinutes,
        FifteenMinutes,
        ThirtyMinutes,
        OneHour,
        TwelveHours,
        TwentyFourHours,
        Never;

        val stringValue: Int
            @StringRes
            get() {
                return when (this) {
                    OneMinute -> R.string.one_minute
                    FiveMinutes -> R.string.five_minutes
                    FifteenMinutes -> R.string.fifteen_minutes
                    ThirtyMinutes -> R.string.thirty_minutes
                    OneHour -> R.string.one_hour
                    TwelveHours -> R.string.twelve_hours
                    TwentyFourHours -> R.string.twenty_four_hours
                    Never -> R.string.never
                }
            }
    }

    enum class ItemListSort(val displayStringId: Int) {
        ALPHABETICALLY(R.string.sort_menu_az),
        RECENTLY_USED(R.string.sort_menu_recent);
    }
}

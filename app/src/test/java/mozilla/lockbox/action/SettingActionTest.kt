/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.action

import mozilla.lockbox.R
import org.junit.Assert
import org.junit.Test

class SettingActionTest {

    @Test
    fun autoLockTime_stringValue() {
        Assert.assertEquals(R.string.one_minute, Setting.AutoLockTime.OneMinute.stringValue)
        Assert.assertEquals(R.string.five_minutes, Setting.AutoLockTime.FiveMinutes.stringValue)
        Assert.assertEquals(R.string.fifteen_minutes, Setting.AutoLockTime.FifteenMinutes.stringValue)
        Assert.assertEquals(R.string.thirty_minutes, Setting.AutoLockTime.ThirtyMinutes.stringValue)
        Assert.assertEquals(R.string.one_hour, Setting.AutoLockTime.OneHour.stringValue)
        Assert.assertEquals(R.string.twelve_hours, Setting.AutoLockTime.TwelveHours.stringValue)
        Assert.assertEquals(R.string.twenty_four_hours, Setting.AutoLockTime.TwentyFourHours.stringValue)
        Assert.assertEquals(R.string.never, Setting.AutoLockTime.Never.stringValue)
    }

    @Test
    fun autoLockTime_secondsValue() {
        Assert.assertEquals(60000, Setting.AutoLockTime.OneMinute.ms)
        Assert.assertEquals(300000, Setting.AutoLockTime.FiveMinutes.ms)
        Assert.assertEquals(900000, Setting.AutoLockTime.FifteenMinutes.ms)
        Assert.assertEquals(1800000, Setting.AutoLockTime.ThirtyMinutes.ms)
        Assert.assertEquals(3600000, Setting.AutoLockTime.OneHour.ms)
        Assert.assertEquals(43200000, Setting.AutoLockTime.TwelveHours.ms)
        Assert.assertEquals(86400000, Setting.AutoLockTime.TwentyFourHours.ms)
        Assert.assertEquals(0, Setting.AutoLockTime.Never.ms)
    }
}
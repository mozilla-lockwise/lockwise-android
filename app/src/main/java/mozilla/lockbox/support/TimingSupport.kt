/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.Setting
import mozilla.lockbox.store.ContextStore
import mozilla.lockbox.store.SettingStore

@ExperimentalCoroutinesApi
open class TimingSupport(
    val settingStore: SettingStore = SettingStore.shared
) : ContextStore {

    companion object {
        val shared by lazy { TimingSupport() }
    }

    private val compositeDisposable = CompositeDisposable()
    private lateinit var preferences: SharedPreferences

    var systemTimingSupport: SystemTimingSupport = DeviceSystemTimingSupport()

    open val shouldLock: Boolean
        get() = lockCurrentlyRequired()

    open val shouldSync: Boolean
        get() = syncCurrentlyRequired()

    val currentTimeMillis: Long
        get() = systemTimingSupport.currentTimeMillis

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun syncCurrentlyRequired(): Boolean {
        val syncTimerDate = preferences.getLong(Constant.Key.syncTimerDate, Long.MAX_VALUE)
        val currentSystemTime = systemTimingSupport.systemTimeElapsed

        // this will be triggered when the system time is very low due to device restarts
        val followingDeviceReboot = (syncTimerDate - currentSystemTime) > Constant.Common.twentyFourHours

        return syncTimerDate < currentSystemTime || followingDeviceReboot
    }

    open fun storeNextAutoLockTime() {
        settingStore.autoLockTime
            .take(1)
            .subscribe(this::updateNextLockTime)
            .addTo(compositeDisposable)
    }

    open fun storeNextSyncTime() {
        storeSyncTimerDate(systemTimingSupport.systemTimeElapsed + Constant.Common.twentyFourHours)
    }

    open fun backdateNextLockTime() {
        storeAutoLockTimerDate(0)
    }

    open fun forwardDateNextLockTime() {
        storeAutoLockTimerDate(Long.MAX_VALUE)
    }

    private fun updateNextLockTime(autoLockTime: Setting.AutoLockTime) {
        if (autoLockTime == Setting.AutoLockTime.Never) {
            storeAutoLockTimerDate(Long.MAX_VALUE)
        } else {
            storeAutoLockTimerDate(systemTimingSupport.systemTimeElapsed + autoLockTime.ms)
        }
    }

    private fun lockCurrentlyRequired(): Boolean {
        val currentSystemTime = systemTimingSupport.systemTimeElapsed
        return if (currentSystemTime <= Constant.Common.sixtySeconds) {
            true
        } else {
            autoLockTimeElapsed()
        }
    }

    private fun autoLockTimeElapsed(): Boolean {
        val autoLockTimerDate = preferences.getLong(Constant.Key.autoLockTimerDate, Long.MAX_VALUE)
        val currentSystemTime = systemTimingSupport.systemTimeElapsed

        return autoLockTimerDate <= currentSystemTime
    }

    private fun storeSyncTimerDate(dateTime: Long) {
        preferences
            .edit()
            .putLong(Constant.Key.syncTimerDate, dateTime)
            .apply()
    }

    private fun storeAutoLockTimerDate(dateTime: Long) {
        preferences
            .edit()
            .putLong(Constant.Key.autoLockTimerDate, dateTime)
            .apply()
    }
}

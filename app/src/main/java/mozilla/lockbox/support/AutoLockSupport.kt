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
class AutoLockSupport(
    val settingStore: SettingStore = SettingStore.shared
) : ContextStore {

    companion object {
        val shared by lazy { AutoLockSupport() }
    }

    private val compositeDisposable = CompositeDisposable()
    private lateinit var preferences: SharedPreferences

    var lockingSupport: LockingSupport = SystemLockingSupport()

    val shouldLock: Boolean
        get() = lockCurrentlyRequired()

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun storeNextAutoLockTime() {
        settingStore.autoLockTime
            .take(1)
            .subscribe(this::updateNextLockTime)
            .addTo(compositeDisposable)
    }

    fun backdateNextLockTime() {
        storeAutoLockTimerDate(0)
    }

    private fun updateNextLockTime(autoLockTime: Setting.AutoLockTime) {
        if (autoLockTime == Setting.AutoLockTime.Never) {
            storeAutoLockTimerDate(Long.MAX_VALUE)
        } else {
            storeAutoLockTimerDate(lockingSupport.systemTimeElapsed + autoLockTime.ms)
        }
    }

    private fun lockCurrentlyRequired(): Boolean {
        return autoLockTimeElapsed()
    }

    private fun autoLockTimeElapsed(): Boolean {
        val autoLockTimerDate = preferences.getLong(Constant.Key.autoLockTimerDate, Long.MAX_VALUE)
        val currentSystemTime = lockingSupport.systemTimeElapsed

        return autoLockTimerDate <= currentSystemTime
    }

    private fun storeAutoLockTimerDate(dateTime: Long) {
        preferences
            .edit()
            .putLong(Constant.Key.autoLockTimerDate, dateTime)
            .apply()
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.preference.PreferenceManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FileReader

class AutoLockStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    val settingStore: SettingStore = SettingStore.shared,
    val lifecycleStore: LifecycleStore = LifecycleStore.shared
) : ContextStore {

    companion object {
        val shared by lazy { AutoLockStore() }
    }

    private val compositeDisposable = CompositeDisposable()
    private lateinit var preferences: SharedPreferences
    private lateinit var currentBootID: String

    val lockRequired: Observable<Boolean> = ReplaySubject.createWithSize(1)

    init {
        lifecycleStore.lifecycleFilter
            .filter { it == LifecycleAction.Foreground }
            .map { lockCurrentlyRequired() }
            .subscribe(lockRequired as Subject)

        lifecycleStore.lifecycleFilter
            .filter { it == LifecycleAction.Background }
            .switchMap { settingStore.autoLockTime.take(1) }
            .subscribe(this::updateNextLockTime)
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)

        currentBootID = FileReader().readContents(Constant.App.bootIDPath)

        preferences
            .edit()
            .putString(Constant.Key.bootID, currentBootID)
            .apply()
    }

    private fun backdateNextLockTime() {
        val currentSystemTime = SystemClock.elapsedRealtime()
        preferences
            .edit()
            .putLong(Constant.Key.autoLockTimerDate, currentSystemTime - 1)
            .apply()
    }

    private fun updateNextLockTime(autoLockTime: Setting.AutoLockTime) {
        val currentSystemTime = SystemClock.elapsedRealtime()
        preferences
            .edit()
            .putLong(Constant.Key.autoLockTimerDate, currentSystemTime + autoLockTime.ms)
            .apply()
    }

    private fun lockCurrentlyRequired(): Boolean {
        return if (onNewBoot()) {
            true
        } else {
            autoLockTimePassed()
        }
    }

    private fun onNewBoot(): Boolean {
        preferences.getString(Constant.Key.bootID, null)?.let {
            return it != currentBootID
        }

        return true
    }

    private fun autoLockTimePassed(): Boolean {
        val autoLockTimerDate = preferences.getLong(Constant.Key.autoLockTimerDate, -1)
        val currentSystemTime = SystemClock.elapsedRealtime()

        val diff = autoLockTimerDate - currentSystemTime

        log.info("current diff: $diff")

        return diff <= 0
    }
}

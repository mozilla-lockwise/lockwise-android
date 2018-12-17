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
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.SimpleFileReader

@ExperimentalCoroutinesApi
class AutoLockStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    val settingStore: SettingStore = SettingStore.shared,
    val lifecycleStore: LifecycleStore = LifecycleStore.shared,
    val dataStore: DataStore = DataStore.shared
) : ContextStore {

    companion object {
        val shared by lazy { AutoLockStore() }
    }

    private val compositeDisposable = CompositeDisposable()
    private lateinit var preferences: SharedPreferences

    private val currentBootID: String
        get() = SimpleFileReader().readContents(Constant.App.bootIDPath)

    val lockRequired: Observable<Boolean> = ReplaySubject.createWithSize(1)

    init {
        lifecycleStore.lifecycleEvents
            .filter { it == LifecycleAction.Foreground }
            .map { lockCurrentlyRequired() }
            .subscribe(lockRequired as Subject)

        Observables.combineLatest(lifecycleStore.lifecycleEvents, dataStore.state)
            .filter { it.first == LifecycleAction.Background && it.second != DataStore.State.Locked }
            .switchMap { settingStore.autoLockTime.take(1) }
            .subscribe(this::updateNextLockTime)
            .addTo(compositeDisposable)

        dispatcher.register
            .filter { it == DataStoreAction.Lock }
            .subscribe {
                this.backdateNextLockTime()
            }
            .addTo(compositeDisposable)

        // wait to store the currentBootId until we can be sure that we've checked it once
        lockRequired
            .take(1)
            .subscribe { storeCurrentBootId() }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun backdateNextLockTime() {
        storeAutoLockTimerDate(SystemClock.elapsedRealtime() - 1)
    }

    private fun updateNextLockTime(autoLockTime: Setting.AutoLockTime) {
        storeAutoLockTimerDate(SystemClock.elapsedRealtime() + autoLockTime.ms)
    }

    private fun lockCurrentlyRequired(): Boolean {
        return onNewBoot() || autoLockTimeElapsed()
    }

    private fun onNewBoot(): Boolean {
        val stored = preferences.getString(Constant.Key.bootID, null) ?: return true

        return !stored.startsWith(currentBootID)
    }

    private fun autoLockTimeElapsed(): Boolean {
        val autoLockTimerDate = preferences.getLong(Constant.Key.autoLockTimerDate, -1)
        val currentSystemTime = SystemClock.elapsedRealtime()

        return autoLockTimerDate <= currentSystemTime
    }

    private fun storeAutoLockTimerDate(dateTime: Long) {
        preferences
            .edit()
            .putLong(Constant.Key.autoLockTimerDate, dateTime)
            .apply()
    }

    private fun storeCurrentBootId() {
        preferences
            .edit()
            .putString(Constant.Key.bootID, currentBootID)
            .apply()
    }
}

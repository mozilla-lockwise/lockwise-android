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
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant

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

    val lockRequired: Observable<Boolean> = ReplaySubject.createWithSize(1)

    init {
        lifecycleStore.lifecycleFilter
            .filter { it == LifecycleAction.Foreground }
            .subscribe {
                checkElapsedTime()
            }
            .addTo(compositeDisposable)

        lifecycleStore.lifecycleFilter
            .filter { it == LifecycleAction.Background }
            .switchMap { settingStore.autoLockTime.take(1) }
            .subscribe {
                val currentSystemTime = SystemClock.elapsedRealtime()
                preferences
                    .edit()
                    .putLong(Constant.Key.autoLockTimerDate, currentSystemTime + it.ms)
                    .apply()
            }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun checkElapsedTime() {
        val autoLockTimerDate = preferences.getLong(Constant.Key.autoLockTimerDate, -1)
        val currentSystemTime = SystemClock.elapsedRealtime()

        // edge case: someone has backgrounded the app after the device has been running for x minutes.
        // they reboot the device and leave it running for x minutes + some time less than autolocktime setting
        // app will be unlocked :(
        val diff = autoLockTimerDate - currentSystemTime

        (lockRequired as Subject).onNext(diff <= 0)
    }
}

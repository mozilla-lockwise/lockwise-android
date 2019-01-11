/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.extensions.debug
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.LockingSupport
import mozilla.lockbox.support.SystemLockingSupport

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

    var lockingSupport: LockingSupport = SystemLockingSupport()

    val activateAutoLockMEGAZORD: Observable<Unit> = PublishSubject.create()

    init {
        // update the lock timer when backgrounding and the datastore is not locked
        lifecycleStore.lifecycleEvents
            .filter { it == LifecycleAction.Background }
            .switchMap { dataStore.state.take(1) }
            .filter { it != DataStore.State.Locked }
            .switchMap { settingStore.autoLockTime.take(1) }
            .subscribe(this::updateNextLockTime)
            .addTo(compositeDisposable)

        // backdate the lock timer when receiving manual lock actions
        dispatcher.register
            // make sure that we are not driving the DataStoreAction.Lock
            .filter { it == DataStoreAction.Lock && !lockCurrentlyRequired() }
            .subscribe {
                this.backdateNextLockTime()
            }
            .addTo(compositeDisposable)

        dispatcher.register
            // getting a credential object means we've logged in
            .filterByType(DataStoreAction::class.java)
            // begin listening for foreground events after login
            .switchMap {
                if (it is DataStoreAction.UpdateCredentials) {
                lifecycleStore.lifecycleEvents
            } else if (it == DataStoreAction.Reset) {
                Observable.empty<LifecycleAction>()
            } else {
                    Observable.empty<LifecycleAction>()
                } }
            .filter { it == LifecycleAction.Foreground }
            // push lockrequired if required
            .map { lockCurrentlyRequired() }
            .debug("lockingCurrentlyRequired")
            .filter { it }
            .map { Unit }
            .subscribe(activateAutoLockMEGAZORD as Subject)
    }

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun backdateNextLockTime() {
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

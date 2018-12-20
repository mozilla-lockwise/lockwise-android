/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant

open class SettingStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) : ContextStore {
    companion object {
        val shared = SettingStore()
    }

    object Keys {
        const val SEND_USAGE_DATA = "send_usage_data"
        const val ITEM_LIST_SORT_ORDER = "sort_order"
        const val UNLOCK_WITH_FINGERPRINT = "unlock_with_fingerprint"
        const val AUTO_LOCK_TIME = "auto_lock_time"
        const val UNLOCK_WITH_FINGERPRINT_PENDING_AUTH = "unlock_with_fingerprint_pending_auth"
    }

    private lateinit var preferences: SharedPreferences
    private val compositeDisposable = CompositeDisposable()

    open val sendUsageData: Observable<Boolean> = ReplaySubject.createWithSize(1)
    open val itemListSortOrder: Observable<Setting.ItemListSort> = ReplaySubject.createWithSize(1)
    open val unlockWithFingerprint: Observable<Boolean> = ReplaySubject.createWithSize(1)
    open val autoLockTime: Observable<Setting.AutoLockTime> = ReplaySubject.createWithSize(1)
    open lateinit var unlockWithFingerprintPendingAuth: Observable<Boolean>

    open val onEnablingFingerprint: Observable<FingerprintAuthAction> =
        dispatcher.register
            .filterByType(FingerprintAuthAction::class.java)

    init {
        val resetObservable = dispatcher.register
            .filter { it == LifecycleAction.UserReset }
            .map { SettingAction.Reset }

        dispatcher.register
            .filterByType(SettingAction::class.java)
            .mergeWith(resetObservable)
            .subscribe {
                val edit = preferences.edit()
                when (it) {
                    is SettingAction.SendUsageData -> {
                        edit.putBoolean(Keys.SEND_USAGE_DATA, it.sendUsageData)
                    }
                    is SettingAction.ItemListSortOrder -> {
                        edit.putString(Keys.ITEM_LIST_SORT_ORDER, it.sortOrder.name)
                    }
                    is SettingAction.UnlockWithFingerprint ->
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT, it.unlockWithFingerprint)
                    is SettingAction.AutoLockTime ->
                        edit.putString(Keys.AUTO_LOCK_TIME, it.time.name)
                    is SettingAction.UnlockWithFingerprintPendingAuth ->
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT_PENDING_AUTH, it.unlockWithFingerprintPendingAuth)
                    is SettingAction.Reset -> {
                        edit.putBoolean(Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
                        edit.putString(Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT, Constant.SettingDefault.unlockWithFingerprint)
                        edit.putString(Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
                    }
                }
                edit.apply()
            }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val rxPrefs = RxSharedPreferences.create(preferences)

        rxPrefs
            .getBoolean(Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
            .asObservable()
            .subscribe(sendUsageData as Subject)

        rxPrefs
            .getString(Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
            .asObservable()
            .map {
                Setting.ItemListSort.valueOf(it)
            }
            .subscribe(itemListSortOrder as Subject)

        rxPrefs
            .getBoolean(Keys.UNLOCK_WITH_FINGERPRINT, Constant.SettingDefault.unlockWithFingerprint)
            .asObservable()
            .subscribe(unlockWithFingerprint as Subject)

        unlockWithFingerprintPendingAuth = rxPrefs.getBoolean(Keys.UNLOCK_WITH_FINGERPRINT_PENDING_AUTH).asObservable()

        rxPrefs
            .getString(Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
            .asObservable()
            .map {
                Setting.AutoLockTime.valueOf(it)
            }
            .subscribe(autoLockTime as Subject)
    }
}
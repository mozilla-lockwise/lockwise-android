/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
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
    val dispatcher: Dispatcher = Dispatcher.shared,
    val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : ContextStore {
    companion object {
        val shared by lazy { SettingStore() }
    }

    object Keys {
        const val SEND_USAGE_DATA = "send_usage_data"
        const val ITEM_LIST_SORT_ORDER = "sort_order"
        const val UNLOCK_WITH_FINGERPRINT = "unlock_with_fingerprint"
        const val AUTO_LOCK_TIME = "auto_lock_time"
        const val DEVICE_SECURITY_PRESENT = "device_security_present"
        const val UNLOCK_WITH_FINGERPRINT_PENDING_AUTH = "unlock_with_fingerprint_pending_auth"
    }

    private lateinit var preferences: SharedPreferences
    @RequiresApi(Build.VERSION_CODES.O)
    private lateinit var autofillManager: AutofillManager
    private val compositeDisposable = CompositeDisposable()

    private val _deviceSecurityWasPresent = ReplaySubject.createWithSize<Boolean>(1)
    private val _autoLockTime = ReplaySubject.createWithSize<Setting.AutoLockTime>(1)

    open val sendUsageData: Observable<Boolean> = ReplaySubject.createWithSize(1)
    open val itemListSortOrder: Observable<Setting.ItemListSort> = ReplaySubject.createWithSize(1)
    open val unlockWithFingerprint: Observable<Boolean> = ReplaySubject.createWithSize(1)
    open val autoLockTime: Observable<Setting.AutoLockTime>
        get() = autoLockSetting()

    open lateinit var unlockWithFingerprintPendingAuth: Observable<Boolean>

    open val onEnablingFingerprint: Observable<FingerprintAuthAction> =
        dispatcher.register
            .filterByType(FingerprintAuthAction::class.java)

    // Accessing these properties in an environment with an Android SDK lower than V26 will result in app crashes!
    open val autofillAvailable: Boolean
        @RequiresApi(Build.VERSION_CODES.O)
        get() = autofillManager.isAutofillSupported

    open val isCurrentAutofillProvider: Boolean
        @RequiresApi(Build.VERSION_CODES.O)
        get() = autofillManager.hasEnabledAutofillServices()

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
                    is SettingAction.SendUsageData ->
                        edit.putBoolean(Keys.SEND_USAGE_DATA, it.sendUsageData)
                    is SettingAction.ItemListSortOrder ->
                        edit.putString(Keys.ITEM_LIST_SORT_ORDER, it.sortOrder.name)
                    is SettingAction.UnlockWithFingerprint ->
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT, it.unlockWithFingerprint)
                    is SettingAction.AutoLockTime ->
                        edit.putString(Keys.AUTO_LOCK_TIME, it.time.name)
                    is SettingAction.UnlockWithFingerprintPendingAuth ->
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT_PENDING_AUTH, it.unlockWithFingerprintPendingAuth)
                    is SettingAction.Autofill ->
                        handleAutofill(it.enable)
                    is SettingAction.Reset -> {
                        edit.putBoolean(Keys.SEND_USAGE_DATA, Constant.SettingDefault.sendUsageData)
                        edit.putString(Keys.ITEM_LIST_SORT_ORDER, Constant.SettingDefault.itemListSort.name)
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT, Constant.SettingDefault.unlockWithFingerprint)
                        edit.putString(Keys.AUTO_LOCK_TIME, Constant.SettingDefault.autoLockTime.name)
                        handleAutofill(false)
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

        val defaultAutoLockTime =
            if (fingerprintStore.isDeviceSecure) Constant.SettingDefault.autoLockTime else Constant.SettingDefault.noSecurityAutoLockTime

        rxPrefs
            .getString(Keys.AUTO_LOCK_TIME, defaultAutoLockTime.name)
            .asObservable()
            .map {
                Setting.AutoLockTime.valueOf(it)
            }
            .subscribe(_autoLockTime)

        rxPrefs
            .getBoolean(Keys.DEVICE_SECURITY_PRESENT, fingerprintStore.isDeviceSecure)
            .asObservable()
            .subscribe(_deviceSecurityWasPresent)

        if (!preferences.contains(Keys.DEVICE_SECURITY_PRESENT)) {
            preferences.edit()
                .putBoolean(Keys.DEVICE_SECURITY_PRESENT, fingerprintStore.isDeviceSecure)
                .apply()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            autofillManager = context.getSystemService(AutofillManager::class.java)
        }
    }

    private fun autoLockSetting() = Observables.combineLatest(_autoLockTime, _deviceSecurityWasPresent)
        .doOnNext {
            if (it.second != fingerprintStore.isDeviceSecure) {
                updateFromDeviceSecurityChange()
            }
        }
        .filter { it.second == fingerprintStore.isDeviceSecure }
        .map { it.first }

    private fun updateFromDeviceSecurityChange() {
        val newAutoLockTime =
            if (fingerprintStore.isDeviceSecure) Constant.SettingDefault.autoLockTime else Constant.SettingDefault.noSecurityAutoLockTime

        val editor = preferences.edit()

        editor.putString(Keys.AUTO_LOCK_TIME, newAutoLockTime.name)
        editor.putBoolean(Keys.DEVICE_SECURITY_PRESENT, fingerprintStore.isDeviceSecure)

        editor.apply()
    }

    private fun handleAutofill(enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        if (!enable && isCurrentAutofillProvider) {
            autofillManager.disableAutofillServices()
        }
    }
}
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
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.model.ItemListSort
import mozilla.lockbox.action.FingerprintAuthAction
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
    }

    private lateinit var preferences: SharedPreferences
    private val compositeDisposable = CompositeDisposable()

    open lateinit var sendUsageData: Observable<Boolean>
    open lateinit var itemListSortOrder: Observable<ItemListSort>
    open lateinit var unlockWithFingerprint: Observable<Boolean>

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
                    is SettingAction.Reset -> {
                        edit.putBoolean(Keys.SEND_USAGE_DATA, Constant.Setting.defaultSendUsageData)
                        edit.putString(Keys.ITEM_LIST_SORT_ORDER, Constant.Setting.defaultItemListSort.name)
                    }
                    is SettingAction.UnlockWithFingerprint ->
                        edit.putBoolean(Keys.UNLOCK_WITH_FINGERPRINT, it.unlockWithFingerprint)
                }
                edit.apply()
            }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val rxPrefs = RxSharedPreferences.create(preferences)

        sendUsageData = rxPrefs
            .getBoolean(Keys.SEND_USAGE_DATA, Constant.Setting.defaultSendUsageData)
            .asObservable()

        itemListSortOrder = rxPrefs
            .getString(Keys.ITEM_LIST_SORT_ORDER, Constant.Setting.defaultItemListSort.name)
            .asObservable()
            .map {
                ItemListSort.valueOf(it)
            }

        unlockWithFingerprint = rxPrefs.getBoolean(Keys.UNLOCK_WITH_FINGERPRINT).asObservable()
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.ItemListSort
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class SettingStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared = SettingStore()
    }

    object Keys {
        const val SEND_USAGE_DATA = "send_usage_data"
        const val ITEM_LIST_SORT_ORDER = "sort_order"
    }

    private lateinit var preferences: SharedPreferences
    private val compositeDisposable = CompositeDisposable()

    open lateinit var sendUsageData: Observable<Boolean>
    open lateinit var itemListSortOrder: Observable<ItemListSort>

    init {
        dispatcher.register
            .filterByType(SettingAction::class.java)
            .subscribe {
                val edit = preferences.edit()
                when (it) {
                    is SettingAction.SendUsageData -> {
                        edit.putBoolean(Keys.SEND_USAGE_DATA, it.sendUsageData)
                    }
                    is SettingAction.ItemListSortOrder -> {
                        edit.putString(Keys.ITEM_LIST_SORT_ORDER, it.sortOrder.name)
                    }
                }
                edit.apply()
            }
            .addTo(compositeDisposable)
    }

    fun apply(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences

        val rxPrefs = RxSharedPreferences.create(sharedPreferences)

        sendUsageData = rxPrefs.getBoolean(Keys.SEND_USAGE_DATA, true).asObservable()

        itemListSortOrder = rxPrefs.getString(Keys.ITEM_LIST_SORT_ORDER, ItemListSort.ALPHABETICALLY.name)
            .asObservable()
            .map {
                ItemListSort.valueOf(it)
            }
    }
}
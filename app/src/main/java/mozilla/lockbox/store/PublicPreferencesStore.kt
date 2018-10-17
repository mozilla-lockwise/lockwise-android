/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemListSort
import mozilla.lockbox.action.SortAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class PublicPreferencesStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    internal val compositeDisposable = CompositeDisposable()
    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        val shared = PublicPreferencesStore()
    }

    private lateinit var itemListSortKey: String
    val itemListSortObservable: Observable<Int> = Observable.create<Int> { emitter ->

        val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == itemListSortKey) {
                emitter.onNext(ItemListSort.idFromOrdinal(sharedPreferences.getInt(itemListSortKey,ItemListSort.ALPHABETICALLY.ordinal)))
            }
        }

        emitter.setCancellable {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
        }

        emitter.onNext(ItemListSort.idFromOrdinal(sharedPrefs.getInt(itemListSortKey,ItemListSort.ALPHABETICALLY.ordinal)))
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    fun applyContext(context: Context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        itemListSortKey = context.resources.getString(R.string.shared_prefs_sort_option_key)

        dispatcher.register
                .filterByType(SortAction::class.java)
                .subscribe {
                    val sortSetting = when (it) {
                        is SortAction.Alphabetically -> { ItemListSort.ALPHABETICALLY.ordinal }
                        is SortAction.RecentlyUsed -> { ItemListSort.RECENTLY_USED.ordinal }
                    }
                    with (sharedPrefs.edit()) {
                        putInt(itemListSortKey, sortSetting)
                        apply()
                    }
                }
                .addTo(compositeDisposable)
    }
}

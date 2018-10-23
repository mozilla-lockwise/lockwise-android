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
import mozilla.lockbox.action.SettingsAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

open class PublicPreferencesStore(val dispatcher: Dispatcher = Dispatcher.shared) {

    internal val compositeDisposable = CompositeDisposable()
    internal lateinit var sharedPrefs: SharedPreferences

    companion object {
        val shared = PublicPreferencesStore()
    }

    private lateinit var itemListSortKey: String
    val itemListSortObservable: Observable<ItemListSort> by lazy {
        val onNext: (String, Int) -> ItemListSort = { key, default ->
            ItemListSort.fromSortId(sharedPrefs.getInt(key,default))!!
        }
        createObservableForKey(itemListSortKey, ItemListSort.ALPHABETICALLY.sortId, onNext)
    }

    fun applyContext(context: Context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        itemListSortKey = context.resources.getString(R.string.shared_prefs_sort_option_key)

        dispatcher.register
                .filterByType(SettingsAction.SortAction::class.java)
                .subscribe {
                   with (sharedPrefs.edit()) {
                        putInt(itemListSortKey, it.id.sortId)
                        apply()
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun <T, R> createObservableForKey(prefsKey: String, default: T, onNext: (String, T) -> R): Observable<R> {
        return Observable.create<R> { emitter ->
            val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == prefsKey) {
                    emitter.onNext(onNext(prefsKey, default))
                }
            }

            emitter.setCancellable {
                sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
            }

            emitter.onNext(onNext(prefsKey, default))
            sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
        }
    }
}

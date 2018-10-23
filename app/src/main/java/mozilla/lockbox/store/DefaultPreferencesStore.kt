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
import mozilla.lockbox.flux.Dispatcher

abstract class DefaultPreferencesStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    internal val compositeDisposable = CompositeDisposable()
    internal lateinit var sharedPrefs: SharedPreferences

    open fun applyContext(context: Context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun <T, R> createObservableForKey(prefsKey: String, default: T, onNext: (String, T) -> R): Observable<R> {
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

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
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

class SettingStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared = SettingStore()
    }

    private val SEND_USAGE_DATA = "send_usage_data"
    private lateinit var preferences: SharedPreferences
    private val compositeDisposable = CompositeDisposable()
    lateinit var sendUsageData: Observable<Boolean>

    init {
        dispatcher.register
            .filterByType(SettingAction::class.java)
            .subscribe {
                when (it) {
                    is SettingAction.SendUsageData -> {
                        preferences.edit().putBoolean(SEND_USAGE_DATA, it.sendUsageData).apply()
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    fun apply(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences

        val rxPrefs = RxSharedPreferences.create(sharedPreferences)

        sendUsageData = rxPrefs.getBoolean(SEND_USAGE_DATA, true).asObservable()
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable

class SettingStore {
    companion object {
        val shared = SettingStore()
    }

    private lateinit var preferences: RxSharedPreferences
    private val SEND_USAGE_DATA = "send_usage_data"

    lateinit var sendUsageData: Observable<Boolean>

    fun apply(sharedPreferences: RxSharedPreferences) {
        preferences = sharedPreferences
        sendUsageData = preferences.getBoolean(SEND_USAGE_DATA, true).asObservable()
    }
}

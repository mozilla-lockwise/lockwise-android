/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import mozilla.lockbox.support.Constant

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (!intent?.action.equals("android.intent.action.BOOT_COMPLETED")) {
            return
        }

        context?.let {
            val prefs = PreferenceManager.getDefaultSharedPreferences(it)

            prefs
                .edit()
                .putLong(Constant.Key.autoLockTimerDate, 0)
                .apply()
        }
    }
}
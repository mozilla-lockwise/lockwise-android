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
import mozilla.lockbox.support.ClipboardSupport
import mozilla.lockbox.support.ClipboardSupportFactory
import mozilla.lockbox.support.Constant

open class LockboxBroadcastReceiver(
    private val createClipboardSupport: ClipboardSupportFactory = ClipboardSupport.create
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Constant.Key.bootCompletedIntent -> resetAutolock(context)
            Constant.Key.clearClipboardIntent -> clearClipboard(context, intent)
        }
    }

    private fun resetAutolock(context: Context?) {
        context?.let {
            val prefs = PreferenceManager.getDefaultSharedPreferences(it)

            prefs
                .edit()
                .putLong(Constant.Key.autoLockTimerDate, 0)
                .apply()
        }
    }

    private fun clearClipboard(context: Context?, intent: Intent?) {
        context?.let {
            val support = createClipboardSupport(it)
            val dirty = intent?.getStringExtra(Constant.Key.clipboardDirtyExtra)

            support.clear(dirty ?: "", "")
        }
    }
}
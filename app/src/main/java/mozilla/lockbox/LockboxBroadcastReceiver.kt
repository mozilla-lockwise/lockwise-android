/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import mozilla.lockbox.support.ClipboardSupport
import mozilla.lockbox.support.ClipboardSupportFactory
import mozilla.lockbox.support.Constant.Key
import mozilla.lockbox.support.Constant.Common.emptyString

open class LockboxBroadcastReceiver(
    private val createClipboardSupport: ClipboardSupportFactory = { ctx: Context -> ClipboardSupport(ctx) }
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Key.bootCompletedIntent -> resetAutolock(context)
            Key.clearClipboardIntent -> clearClipboard(context, intent)
        }
    }

    private fun resetAutolock(context: Context?) {
        context?.let {
            val prefs = PreferenceManager.getDefaultSharedPreferences(it)

            prefs
                .edit()
                .putLong(Key.autoLockTimerDate, 0)
                .apply()
        }
    }

    private fun clearClipboard(context: Context?, intent: Intent?) {
        context?.let {
            val support = createClipboardSupport(it)
            val dirty = intent?.getStringExtra(Key.clipboardDirtyExtra)

            support.clear(dirty ?: emptyString)
        }
    }
}
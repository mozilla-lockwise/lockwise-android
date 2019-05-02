/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import mozilla.lockbox.support.Constant.Common.emptyString

typealias ClipboardSupportFactory = (Context) -> ClipboardSupport

open class ClipboardSupport(
    context: Context
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    open fun paste(label: String, value: String) {
        clipboard.primaryClip = ClipData.newPlainText(label, value)
    }

    open fun clear(dirty: String) {
        if (dirty == emptyString) { return }

        val clipData = clipboard.primaryClip?.getItemAt(0)
        if (clipData?.text == dirty) {
            clipboard.primaryClip = ClipData.newPlainText(emptyString, emptyString)
        }
    }
}
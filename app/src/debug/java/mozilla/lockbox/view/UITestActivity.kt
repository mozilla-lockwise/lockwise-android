/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import mozilla.lockbox.R
import mozilla.lockbox.log

class UITestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
    }

    fun launchFingerprint(@Suppress("UNUSED_PARAMETER") view: View) {
        val dialogFragment = FingerprintAuthDialogFragment()
        val fragmentManager = this.supportFragmentManager
        try {
            dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(
                R.string.enable_fingerprint_dialog_title,
                R.string.enable_fingerprint_dialog_subtitle
            )
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }
}

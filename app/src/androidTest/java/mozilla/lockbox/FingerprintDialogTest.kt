/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import mozilla.lockbox.robots.fingerprintDialog
import mozilla.lockbox.robots.uiComponents
import mozilla.lockbox.view.UITestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example test to demonstrate a test activity and some UI Components from the main app.
 */
@RunWith(AndroidJUnit4::class)
open class FingerprintDialogTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(UITestActivity::class.java)

    @Test
    fun testLaunchRobot() {
        uiComponents {
            launchFingerprintDialog()
        }

        fingerprintDialog {
            exists()
            // touchFingerprint() // does not work.
            tapCancel()
        }

        uiComponents {
            exists()
        }
    }
}

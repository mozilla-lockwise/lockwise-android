/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R

// Fingerprint Dialog
class FingerprintDialogRobot : BaseTestRobot {
    override fun exists() = displayed { id(R.id.fingerprintStatus) }

    fun touchFingerprint(finger: String = "1") {
        Runtime.getRuntime().exec("adb -e emu finger $finger")
    }

    fun tapCancel() = click { id(R.id.cancel) }
}
fun fingerprintDialog(f: FingerprintDialogRobot.() -> Unit) = FingerprintDialogRobot().apply(f)

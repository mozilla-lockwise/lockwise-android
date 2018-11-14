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
class UIComponentRobot : BaseTestRobot {
    override fun exists() = displayed { id(R.id.uiComponents) }

    fun launchFingerprintDialog() = click { id(R.id.button_launch_fingerprint) }

    fun launchEnableFingerprintDialog() = click { id(R.id.button_enable_fingerprint) }
}

fun uiComponents(f: UIComponentRobot.() -> Unit) = UIComponentRobot().apply(f)
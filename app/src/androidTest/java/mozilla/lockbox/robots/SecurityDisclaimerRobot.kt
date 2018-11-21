/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R

// SecurityDisclaimer
class SecurityDisclaimerRobot : BaseTestRobot {
    override fun exists() = displayed { text(R.string.no_device_security_title) }

    fun tapSetUp() = click { text(R.string.set_up_security_button) }

    fun cancel() = click { text(R.string.cancel) }
}

fun securityDisclaimer(f: SecurityDisclaimerRobot.() -> Unit) = SecurityDisclaimerRobot().apply(f)

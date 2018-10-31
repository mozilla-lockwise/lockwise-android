/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R

class WelcomeRobot: BaseTestRobot {
    override fun exists() = displayed { id(R.id.buttonGetStarted) }

    fun tapGetStarted() = click { id(R.id.buttonGetStarted) }
}

fun welcome(f: WelcomeRobot.() -> Unit) = WelcomeRobot().apply(f)
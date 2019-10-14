/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import kotlinx.android.synthetic.main.mtrl_alert_dialog_actions.view.*
import mozilla.lockbox.R

class WelcomeRobot : BaseTestRobot {
    override fun exists() = displayed { id(R.id.buttonGetStartedManually) }

    fun tapGetStarted() = click { id(R.id.buttonGetStartedManually) }

    fun tapSkipSecureYourDevice() = click { id(android.R.id.button2) }
}

fun welcome(f: WelcomeRobot.() -> Unit) = WelcomeRobot().apply(f)
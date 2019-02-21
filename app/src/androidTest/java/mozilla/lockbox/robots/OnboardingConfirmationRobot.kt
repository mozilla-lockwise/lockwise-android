/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R

class OnboardingConfirmationRobot : BaseTestRobot {
    override fun exists() = displayed { text(R.string.all_set) }

    fun clickFinish() = click { id(R.id.finishButton) }
}

fun onboardingConfirmationScreen(f: OnboardingConfirmationRobot.() -> Unit) = OnboardingConfirmationRobot().apply(f)
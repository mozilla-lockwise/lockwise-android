/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import mozilla.lockbox.R

// DeleteCredentialDisclaimer
class DeleteCredentialDisclaimerRobot : BaseTestRobot {

    override fun exists() = VisibilityAssertions.displayed { id(R.id.alertTitle) }

        fun tapCancelButton() = ClickActions.click { text(R.string.cancel) }
        fun tapDeleteButton() = ClickActions.click { text("DELETE") }
    }

fun deleteCredentialDisclaimer(f: DeleteCredentialDisclaimerRobot.() -> Unit) = DeleteCredentialDisclaimerRobot().apply(f)
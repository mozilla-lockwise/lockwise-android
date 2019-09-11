/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import mozilla.lockbox.R

// KebabMenu
class KebabMenuRobot : BaseTestRobot {
    override fun exists() = VisibilityAssertions.displayed { text("Delete") }

    fun tapEditButton() = ClickActions.click { text(R.string.edit) }
    fun tapDeleteButton() = ClickActions.click { text(R.string.delete) }
}

fun kebabMenu(f: KebabMenuRobot.() -> Unit) = KebabMenuRobot().apply(f)

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R
import mozilla.lockbox.view.RootActivity
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_API_USAGE_ERROR", "EXPERIMENTAL_IS_NOT_ENABLED")
class ItemDetailRobot : BaseTestRobot {
    override fun exists() = displayed {
        id(R.id.inputHostname)
        id(R.id.inputUsername)
        id(R.id.inputPassword)
    }

    fun tapCopyUsername() = ClickActions.click { id(R.id.btnUsernameCopy) }
    fun tapCopyPass() = ClickActions.click { id(R.id.btnPasswordCopy) }

    fun toastIsDisplayed(id: Int, activityRule: ActivityTestRule<RootActivity>) =
        onView(withText(id)).inRoot(withDecorView(not(`is`(activityRule.activity.window.decorView))))
            .check(matches(isDisplayed()))

    fun tapKebabMenu() = ClickActions.click { id(R.id.kebabMenuButton) }
}

fun itemDetail(f: ItemDetailRobot.() -> Unit) = ItemDetailRobot().apply(f)
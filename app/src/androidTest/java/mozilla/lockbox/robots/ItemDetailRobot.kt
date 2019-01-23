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

// ItemDetail
class ItemDetailRobot : BaseTestRobot {
    override fun exists() = displayed {
        id(R.id.inputHostname)
        id(R.id.btnUsernameCopy)
        id(R.id.inputUsername)
        id(R.id.inputPassword)
        id(R.id.btnPasswordCopy)
    }

    fun tapCopyUsername() = ClickActions.click { id(R.id.btnUsernameCopy) }
    fun tapCopyPass() = ClickActions.click { id(R.id.btnPasswordCopy) }

    fun toastIsDisplayed(id: Int, activityRule: ActivityTestRule<RootActivity>) =
        onView(withText(id)).inRoot(withDecorView(not(`is`(activityRule.activity.getWindow().decorView))))
            .check(matches(isDisplayed()))
}

fun itemDetail(f: ItemDetailRobot.() -> Unit) = ItemDetailRobot().apply(f)
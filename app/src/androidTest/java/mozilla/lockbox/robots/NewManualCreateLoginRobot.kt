/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.withId
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import mozilla.lockbox.R
import org.hamcrest.Matchers.not

class NewManualCreateLoginRobot : BaseTestRobot {

    override fun exists() = VisibilityAssertions.displayed { id(R.id.toolbarTitle) }

    fun newUserName(text: String) = onView(withId(R.id.inputUsername)).perform(replaceText(text))
    fun newPassword(text: String) = onView(withId(R.id.inputPassword)).perform(replaceText(text))
    fun newHostname(text: String) = onView(withId(R.id.inputHostname)).perform(replaceText(text))

    fun assertErrorEmptyHostname() = VisibilityAssertions.displayed { text("Hostname cannot be empty") }

    fun assertErrorWrongHostname() = VisibilityAssertions.displayed { text("Web address must contain “https://“ or “http://“") }

    fun assertErrorEmptyPassord() = VisibilityAssertions.displayed { text("Password cannot be empty") }

    fun saveButtonIsClickable() = onView(withId(R.id.saveEntryButton)).check(matches(isClickable()))
    fun saveButtonIsNotClickable() = onView(withId(R.id.saveEntryButton)).check(matches(not(isClickable())))
}

fun newManualCreateLogin(f: NewManualCreateLoginRobot.() -> Unit) = NewManualCreateLoginRobot().apply(f)

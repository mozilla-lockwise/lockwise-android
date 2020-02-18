/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.ViewMatchers.withId
import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import mozilla.lockbox.R

class EditCredentialRobot : BaseTestRobot {

    override fun exists() = VisibilityAssertions.displayed { id(R.id.toolbarTitle) }

    fun saveChanges() = ClickActions.click { id(R.id.saveEntryButton) }
    fun closeEditChanges() = ClickActions.click { contentDescription("Back") }

    fun tapOnUserName() = onView(withId(R.id.inputUsername)).perform(click())
    fun tapOnPasswordRemoveChar() = onView(withId(R.id.inputPassword))
            .perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_DEL))
    fun editUserName(text: String) = onView(withId(R.id.inputUsername)).perform(replaceText(text))

    fun editPassword(text: String) = onView(withId(R.id.inputPassword))
            .perform(replaceText(text))

    fun assertErrorEmptyPassord() = VisibilityAssertions.displayed { id(R.id.textinput_error) }
    fun noErrorEmptyPassword() = VisibilityAssertions.notDisplayed { id(R.id.textinput_error) }
}

fun editCredential(f: EditCredentialRobot.() -> Unit) = EditCredentialRobot().apply(f)

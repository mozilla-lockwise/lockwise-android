/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import br.com.concretesolutions.kappuccino.custom.recyclerView.RecyclerViewInteractions.recyclerView
import junit.framework.Assert

interface BaseTestRobot {
    fun exists()

    fun fillEditText(resId: Int, text: String): ViewInteraction =
        onView(withId(resId)).perform(replaceText(text), closeSoftKeyboard())

    fun textView(resId: Int): ViewInteraction = onView(withId(resId))

    fun matchText(viewInteraction: ViewInteraction, text: String): ViewInteraction = viewInteraction
        .check(matches(withText(text)))

    fun matchText(resId: Int, text: String): ViewInteraction = matchText(textView(resId), text)

    fun clickListItem(listRes: Int, position: Int) {
        recyclerView(listRes) {
            atPosition(position) { click() }
        }
    }

    fun swipeDown(resId: Int): ViewInteraction =
        onView(withId(resId)).perform(swipeDown())

    fun back(remainInApplication: Boolean = true) {
        Espresso.closeSoftKeyboard()
        try {
            Espresso.pressBack()
            Assert.assertTrue("Expected to be still in the app, but aren't", remainInApplication)
        } catch (e: NoActivityResumedException) {
            Assert.assertFalse("Expected to have left the app, but haven't", remainInApplication)
        }
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.NoActivityResumedException
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.closeSoftKeyboard
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
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
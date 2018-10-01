/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions.open
import android.support.test.espresso.contrib.NavigationViewActions.navigateTo
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId

class Navigator {
    fun gotoFxALogin() {
        onView(withId(R.id.buttonGetStarted)).perform(click())
        onView(withId(R.id.logMeInButton)).check(matches(isDisplayed()))
    }

    fun gotoItemList() {
        gotoFxALogin()
        onView(withId(R.id.logMeInButton)).perform(click())
        onView(withId(R.id.appDrawer)).check(matches(isDisplayed()))
    }

    fun gotoItemList_openMenu() {
        gotoItemList()
        val drawer = onView(withId(R.id.appDrawer))
        drawer.check(matches(isDisplayed()))
        drawer.perform(open())
    }

    fun gotoSettings() {
        gotoItemList_openMenu()
        onView(withId(R.id.navView)).perform(navigateTo(R.id.goto_settings))
        onView(withId(R.id.settings_placeholder)).check(matches(isDisplayed()))
    }

    fun gotoItemDetail() {
        gotoItemList()
        onView(withId(R.id.entriesView)).perform(click())
        onView(withId(R.id.inputHostname)).check(matches(isDisplayed()))
    }
}
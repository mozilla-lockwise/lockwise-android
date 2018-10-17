/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.NoActivityResumedException
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions.open
import android.support.test.espresso.contrib.NavigationViewActions.navigateTo
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import junit.framework.Assert

class Navigator {
    fun gotoFxALogin() {
        onView(withId(R.id.buttonGetStarted)).perform(click())
        checkAtFxALogin()
    }

    fun checkAtFxALogin() {
        onView(withId(R.id.logMeInButton)).check(matches(isDisplayed()))
    }

    fun gotoItemList() {
        gotoFxALogin()
        onView(withId(R.id.logMeInButton)).perform(click())
        checkAtItemList()
    }

    fun checkAtItemList() {
        onView(withId(R.id.filterButton)).check(matches(isDisplayed()))
        onView(withId(R.id.appDrawer)).check(matches(isDisplayed()))
    }

    fun gotoItemList_filter() {
        gotoItemList()
        onView(withId(R.id.filterButton)).perform(click())
        checkAtFilterList()
    }

    fun checkAtFilterList() {
        onView(withId(R.id.filterField)).check(matches(isDisplayed()))
    }

    fun gotoItemList_openMenu() {
        gotoItemList()
        val drawer = onView(withId(R.id.appDrawer))
        drawer.check(matches(isDisplayed()))
        drawer.perform(open())
    }

    fun gotoSettings() {
        gotoItemList_openMenu()
        onView(withId(R.id.navView)).perform(navigateTo(R.id.action_itemList_to_setting))
        checkAtSettings()
    }

    fun checkAtSettings() {
        onView(withId(R.id.settings_placeholder)).check(matches(isDisplayed()))
    }

    fun gotoLockScreen() {
        gotoItemList_openMenu()
        onView(withId(R.id.navView)).perform(navigateTo(R.id.action_itemList_to_locked))
        checkAtLockScreen()
    }

    private fun checkAtLockScreen() {
        onView(withId(R.id.unlockButton)).check(matches(isDisplayed()))
    }

    fun gotoItemDetail() {
        gotoItemList()
        gotoItemDetail_from_itemList()
    }

    fun gotoItemDetail_from_itemList() {
        onView(withId(R.id.entriesView)).perform(click())
        checkAtItemDetail()
    }

    fun checkAtItemDetail() {
        onView(withId(R.id.inputHostname)).check(matches(isDisplayed()))
    }

    fun checkOnWelcome() {
        onView(withId(R.id.buttonGetStarted)).check(matches(isDisplayed()))
    }

    fun back(remainInApplication: Boolean = true) {
        closeSoftKeyboard()
        try {
            pressBack()
            Assert.assertTrue("Expected to be still in the app, but aren't", remainInApplication)
        } catch (e: NoActivityResumedException) {
            Assert.assertFalse("Expected to have left the app, but haven't", remainInApplication)
        }
    }
}
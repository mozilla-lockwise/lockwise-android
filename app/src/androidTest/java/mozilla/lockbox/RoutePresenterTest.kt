/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import mozilla.lockbox.view.RootActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
open class RoutePresenterTest {
    private val navigator = Navigator()

    @Rule @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("mozilla.lockbox", appContext.packageName)
    }

    @Test
    fun testFxALogin() {
        navigator.gotoFxALogin()
        navigator.back()
        navigator.checkOnWelcome()
    }

    @Test
    fun testItemList() {
        navigator.gotoItemList()
        navigator.back(false)
    }

    @Test
    fun testFilterList() {
        navigator.gotoItemList_filter()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testFilterToItemDetail() {
        navigator.gotoItemList_filter()
        navigator.gotoItemDetail_from_itemList()
        navigator.back()
        navigator.checkAtFilterList()
    }

    @Test
    fun testSettings() {
        navigator.gotoSettings()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testItemDetail() {
        navigator.gotoItemDetail()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testLockScreen() {
        navigator.gotoLockScreen()
        navigator.back(false)
    }
}

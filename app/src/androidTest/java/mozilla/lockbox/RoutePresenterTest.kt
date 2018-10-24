/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import mozilla.lockbox.robots.filteredItemList
import mozilla.lockbox.view.RootActivity
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
        filteredItemList {
            selectItem(0)
            back()
        }
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
    fun testNoSecurityDialog() {
        navigator.gotoNoSecurityDialog()
        navigator.back(true)
        navigator.checkAtItemList()
    }

    @Test
    fun testNoSecurityDialogSetupSecurity() {
        navigator.goToSecuritySettings()
    }
}

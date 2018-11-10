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
import org.junit.Before
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

    @Before
    fun setUp() {
        navigator.reset()
    }

    @Test
    fun testFxALogin() {
        log.info("new test!")
        navigator.gotoFxALogin()
        navigator.back()
        navigator.checkOnWelcome()
    }

    @Test
    fun testItemList() {
        log.info("new test!")
        navigator.gotoItemList()
        navigator.back(false)
    }

    @Test
    fun testFilterList() {
        log.info("new test!")
        navigator.gotoItemList_filter()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testFilterToItemDetail() {
        log.info("new test!")
        navigator.gotoItemList_filter()
        filteredItemList {
            selectItem(0)
            navigator.checkAtItemDetail()
            back()
        }
        navigator.checkAtFilterList()
    }

    @Test
    fun testSettings() {
        log.info("new test!")
        navigator.gotoSettings()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testAccountSetting() {
        log.info("new test!")
        navigator.gotoAccountSetting()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testItemDetail() {
        log.info("new test!")
        navigator.gotoItemDetail()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testNoSecurityDialog() {
        log.info("new test!")
        navigator.gotoNoSecurityDialog()
        navigator.back(true)
        navigator.checkAtItemList()
    }

    @Test
    fun testNoSecurityDialogSetupSecurity() {
        log.info("new test!")
        navigator.goToSecuritySettings()
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.robots.disconnectDisclaimer
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
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        Thread.sleep(200L)
        Dispatcher.shared.dispatch(LifecycleAction.UserReset)
        Thread.sleep(200L)
    }

    @Test
    fun testFxALogin() {
        navigator.gotoFxALogin()
        navigator.back()
        navigator.checkOnWelcome()
    }

    @Test
    fun testItemList() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoItemList(true)
        navigator.back(false)
    }

    @Test
    fun testFilterList() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoItemList_filter()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testFilterToItemDetail() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoItemList_filter()
        filteredItemList {
            selectItem(0)
            back()
        }
        navigator.checkAtFilterList()
    }

    @Test
    fun testSettings() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoSettings()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testAccountSetting() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoAccountSetting()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testDisconnectDisclaimer() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoDisconnectDisclaimer()
        navigator.back()
        navigator.checkAtAccountSetting()
    }

    @Test
    fun testDisconnecting() {
        // note: this won't work until routing PR is merged
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoDisconnectDisclaimer()
        disconnectDisclaimer { tapDisconnect() }
        navigator.checkAtWelcome()
    }

    @Test
    fun testItemDetail() {
        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
        navigator.gotoItemDetail()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testNoSecurityDialog() {
//        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
//        navigator.gotoNoSecurityDialog()
//        navigator.back(true)
//        navigator.checkAtItemList()
    }

    @Test
    fun testNoSecurityDialogSetupSecurity() {
//        Dispatcher.shared.dispatch(DataStoreAction.Unlock)
//        navigator.goToSecuritySettings()
    }
}

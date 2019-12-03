/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.uiTests

import androidx.test.rule.ActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.robots.disconnectDisclaimer
import mozilla.lockbox.robots.filteredItemList
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.view.RootActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
open class AppRoutePresenterTest {
    private val navigator = Navigator()

    @Rule @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Ignore("619-ui-tests-bitrise (#620)")
    @Test
    fun testFingerprintOnboarding() {
        if (FingerprintStore.shared.isFingerprintAuthAvailable) {
            navigator.gotoFingerprintOnboarding()
            navigator.checkAtFingerprintOnboarding()
        }
    }

    @Test
    fun testItemList() {
        navigator.gotoItemList(false)
        navigator.checkAtItemList()
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
        // navigator.back()
    }

    @Test
    fun testSettings() {
        navigator.gotoSettings()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testAccountSetting() {
        navigator.gotoAccountSetting()
        navigator.back()
        navigator.checkAtItemList()
    }

    @Test
    fun testDisconnectDisclaimer() {
        navigator.gotoDisconnectDisclaimer()
        navigator.back()
        navigator.checkAtAccountSetting()
        navigator.back()
    }

    @Ignore("619-ui-tests-bitrise (#620)")
    @Test
    fun testDisconnecting() {
        navigator.gotoDisconnectDisclaimer()
        disconnectDisclaimer { tapDisconnect() }
        navigator.checkAtWelcome()
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

    @Ignore("619-ui-tests-bitrise (#620)")
    @Test
    fun testNoSecurityDialogSetupSecurity() {
//        navigator.goToSecuritySettings()
    }
}

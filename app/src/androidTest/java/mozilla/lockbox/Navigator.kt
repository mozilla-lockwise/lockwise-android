/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.provider.Settings
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.intent.Intents
import androidx.test.rule.ActivityTestRule
import br.com.concretesolutions.kappuccino.custom.intent.IntentMatcherInteractions.sentIntent
import br.com.concretesolutions.kappuccino.custom.intent.IntentMatcherInteractions.stubIntent
import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.robots.accountSettingScreen
import mozilla.lockbox.robots.disconnectDisclaimer
import mozilla.lockbox.robots.filteredItemList
import mozilla.lockbox.robots.fxaLogin
import mozilla.lockbox.robots.itemDetail
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.robots.lockScreen
import mozilla.lockbox.robots.securityDisclaimer
import mozilla.lockbox.robots.settings
import mozilla.lockbox.robots.welcome
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.RootActivity
import org.junit.Assert

@ExperimentalCoroutinesApi
class Navigator {
    fun resetApp(activityRule: ActivityTestRule<RootActivity>) {
        activityRule.activity.runOnUiThread {
            activityRule.activity.recreate()
        }
    }

    fun disconnectAccount() {
        val subject = DataStore.shared.state as ReplaySubject<DataStore.State>
        when (subject.value) {
            is DataStore.State.Locked -> {
                Dispatcher.shared.dispatch(DataStoreAction.Unlock)
                blockUntil(DataStore.State.Unlocked)
            }

            is DataStore.State.Unprepared -> {
                return
            }

            else -> {
                // NOP
            }
        }

        Dispatcher.shared.dispatch(LifecycleAction.UserReset)
        blockUntil(DataStore.State.Unprepared)
        checkAtWelcome()
    }

    fun blockUntil(vararg states: RouteAction) {
        blockUntil(RouteStore.shared.routes, *states)
    }

    fun blockUntil(vararg states: DataStore.State) {
        blockUntil(DataStore.shared.state, *states)
    }

    fun <T> blockUntil(observable: Observable<T>, vararg states: T) {
        val stateSubject = observable as? ReplaySubject
        if (stateSubject != null && states.contains(stateSubject.value)) {
            return
        }
        observable.filter { states.contains(it) }.blockingNext().iterator().next()
    }

    fun gotoFxALogin() {
        welcome { tapGetStarted() }
        checkAtFxALogin()
    }

    fun checkAtFxALogin() {
        fxaLogin { exists() }
    }

    fun checkAtWelcome() {
        welcome { exists() }
    }

    fun gotoItemList(goManually: Boolean = false) {
        if (goManually) {
            gotoFxALogin()
            fxaLogin { tapPlaceholderLogin() }
        } else {
            Dispatcher.shared.dispatch(LifecycleAction.UseTestData)
            log.info("blocking for the routes")
            blockUntil(RouteStore.shared.routes, RouteAction.ItemList)
        }
        checkAtItemList()
    }

    fun checkAtItemList() {
        itemList { exists() }
    }

    fun gotoItemList_filter() {
        gotoItemList()
        itemList { tapFilterList() }
        checkAtFilterList()
    }

    fun checkAtFilterList() {
        filteredItemList { exists() }
    }

    fun gotoSettings() {
        gotoItemList()
        itemList { tapSettings() }
        checkAtSettings()
    }

    private fun checkAtSettings() {
        settings { exists() }
    }

    fun gotoAccountSetting() {
        gotoItemList()
        itemList { tapAccountSetting() }
        checkAtAccountSetting()
    }

    fun checkAtAccountSetting() {
        accountSettingScreen { exists() }
    }

    fun gotoNoSecurityDialog() {
        gotoItemList()
        itemList { tapLockNow() }
        checkAtSecurityDialog()
    }

    private fun checkAtSecurityDialog() {
        securityDisclaimer { exists() }
    }

    fun goToSecuritySettings() {
        gotoNoSecurityDialog()
        Intents.init()
        stubSystemSecuritySettingIntent()
        securityDisclaimer { tapSetUp() }
        listenSystemSecuritySettingIntent()
        Intents.release()
    }

    private fun stubSystemSecuritySettingIntent() {
        stubIntent {
            action(Settings.ACTION_SECURITY_SETTINGS)
            respondWith {
                ok()
            }
        }
    }

    private fun listenSystemSecuritySettingIntent() {
        sentIntent {
            action(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun gotoDisconnectDisclaimer() {
        gotoAccountSetting()
        accountSettingScreen { tapDisconnect() }
        checkAtDisconnectDisclaimer()
    }

    private fun checkAtDisconnectDisclaimer() {
        disconnectDisclaimer { exists() }
    }

    @Suppress("unused")
    fun goToLockScreen() {
        // not called until we can stub FingerprintStore in tests
        gotoItemList()
        itemList { tapLockNow() }
        checkAtLockScreen()
    }

    fun checkAtLockScreen() {
        lockScreen { exists() }
    }

    fun gotoItemDetail(position: Int = 0) {
        gotoItemList()
        gotoItemDetail_from_itemList(position)
    }

    private fun gotoItemDetail_from_itemList(position: Int = 0) {
        itemList { selectItem(position) }
        checkAtItemDetail()
    }

    private fun checkAtItemDetail() {
        itemDetail { exists() }
    }

    fun back(remainInApplication: Boolean = true) {
        closeSoftKeyboard()
        try {
            pressBack()
            Assert.assertTrue("Expected to have left, but haven't", remainInApplication)
        } catch (e: NoActivityResumedException) {
            Assert.assertFalse("Expected to still be in the app, but aren't", remainInApplication)
        }
    }
}
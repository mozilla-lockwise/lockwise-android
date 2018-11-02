/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.support.test.espresso.Espresso.closeSoftKeyboard
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.NoActivityResumedException
import junit.framework.Assert
import mozilla.lockbox.robots.filteredItemList
import mozilla.lockbox.robots.fxaLogin
import mozilla.lockbox.robots.itemDetail
import mozilla.lockbox.robots.itemList
import mozilla.lockbox.robots.lockScreen
import mozilla.lockbox.robots.settings
import mozilla.lockbox.robots.welcome

class Navigator {
    fun gotoFxALogin() {
        welcome { tapGetStarted() }
        checkAtFxALogin()
    }

    fun checkAtFxALogin() {
        fxaLogin { exists() }
    }

    fun gotoItemList() {
        gotoFxALogin()
        fxaLogin { tapPlaceholderLogin() }
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

    fun checkAtSettings() {
        settings { exists() }
    }

    fun gotoLockScreen() {
        gotoItemList()
        itemList { tapLockNow() }
        checkAtLockScreen()
    }

    private fun checkAtLockScreen() {
        lockScreen { exists() }
    }

    fun gotoItemDetail(position: Int = 0) {
        gotoItemList()
        gotoItemDetail_from_itemList(position)
    }

    fun gotoItemDetail_from_itemList(position: Int = 0) {
        itemList { selectItem(position) }
        checkAtItemDetail()
    }

    fun checkAtItemDetail() {
        itemDetail { exists() }
    }

    fun checkOnWelcome() {
        welcome { exists() }
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
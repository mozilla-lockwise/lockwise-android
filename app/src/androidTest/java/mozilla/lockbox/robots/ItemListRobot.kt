/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions.navigateTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R

// ItemList
class ItemListRobot : BaseTestRobot {
    override fun exists() = displayed {
        id(R.id.filterButton)
        id(R.id.sortButton)
        id(R.id.appDrawer)
    }

    fun sortMenuIsDisplayed() = displayed {
        text(R.string.sort_menu_az)
        text(R.string.sort_menu_recent)
    }

    fun spinnerDisplaysFirstItemSelection() = displayed {
        id(R.id.sortButton)
        text(R.string.all_logins_a_z)
    }

    fun spinnerDisplaysSecondItemSelection() = displayed {
        id(R.id.sortButton)
        text(R.string.all_logins_recent)
    }

    fun editedCredentialHostnameExists(hostname: String) = displayed { text(hostname) }
    fun editedCredentialUsernameExists(username: String) = displayed { text(username) }

    fun openCredential(credential: String) = click { allOf { text(credential) } }
    fun credentialRemovedDoesNotExist(hostname: String) = onView(withText(hostname)).check(doesNotExist())

    fun openMenu(): ViewInteraction {
        val drawer = onView(withId(R.id.appDrawer))
        drawer.perform(DrawerActions.open())
        return onView(withId(R.id.navView))!!
    }

    private fun menuOption(item: Int) = openMenu().perform(navigateTo(item))

    fun tapSortButton() = click { id(R.id.sortButton) }

    fun selectFirstItemInSortMenu() = click { allOf { text(R.string.sort_menu_az) } }

    fun selectSecondItemInSortMenu() = click { allOf { text(R.string.sort_menu_recent) } }

    fun tapFilterList() = click { id(R.id.filterButton) }

    fun tapSettings() = menuOption(R.id.setting_menu_item)

    fun tapLockNow() {
        openMenu()
        click { id(R.id.lockNow) }
    }

    fun tapAccountSetting() = menuOption(R.id.account_setting_menu_item)

    fun selectItem(position: Int = 0) = clickListItem(R.id.entriesView, position)

    fun pullToRefresh() = swipeDown(R.id.entriesView)
}

fun itemList(f: ItemListRobot.() -> Unit) = ItemListRobot().apply(f)
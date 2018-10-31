/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.contrib.NavigationViewActions.navigateTo
import android.support.test.espresso.matcher.ViewMatchers.withId
import br.com.concretesolutions.kappuccino.actions.ClickActions.click
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import mozilla.lockbox.R

// ItemList
class ItemListRobot : BaseTestRobot {
    override fun exists() = displayed {
        id(R.id.filterButton)
        id(R.id.appDrawer)
    }

    fun openMenu(): ViewInteraction {
        val drawer = onView(withId(R.id.appDrawer))
        drawer.perform(DrawerActions.open())
        return onView(withId(R.id.navView))!!
    }

    private fun menuOption(item: Int) = openMenu().perform(navigateTo(item))

    fun tapFilterList() = click { id(R.id.filterButton) }

    fun tapSettings() = menuOption(R.id.fragment_setting)

    fun tapLockNow() = menuOption(R.id.fragment_locked)

    fun selectItem(position: Int = 0) = clickListItem(R.id.entriesView, position)
}

fun itemList(f: ItemListRobot.() -> Unit) = ItemListRobot().apply(f)
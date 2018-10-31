/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.robots

import br.com.concretesolutions.kappuccino.actions.TextActions
import br.com.concretesolutions.kappuccino.actions.TextActions.typeText
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions
import br.com.concretesolutions.kappuccino.assertions.VisibilityAssertions.displayed
import br.com.concretesolutions.kappuccino.custom.recyclerView.RecyclerViewInteractions
import br.com.concretesolutions.kappuccino.custom.recyclerView.RecyclerViewInteractions.recyclerView
import mozilla.lockbox.R

// Filter ItemList
class FilteredItemListRobot: BaseTestRobot {
    override fun exists() = displayed { id(R.id.filterField) }

    fun typeFilterText(text: String) = typeText(text) { id(R.id.filterField) }

    fun selectItem(position: Int = 0) = clickListItem(R.id.entriesView, position)
}

fun filteredItemList(f: FilteredItemListRobot.() -> Unit) = FilteredItemListRobot().apply(f)
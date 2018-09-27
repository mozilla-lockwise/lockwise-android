/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.list_cell_item.itemSubtitle
import kotlinx.android.synthetic.main.list_cell_item.itemTitle
import mozilla.lockbox.model.ItemViewModel

class ItemViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    var itemViewModel: ItemViewModel? = null
        set(value) {
            field = value
            value?.let {
                itemTitle.text = it.title
                itemSubtitle.text = it.subtitle
            }
        }

}
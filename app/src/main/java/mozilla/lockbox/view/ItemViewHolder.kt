/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.view.View
import kotlinx.android.synthetic.main.list_cell_item.itemTitle
import kotlinx.android.synthetic.main.list_cell_item.itemSubtitle
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemListCell
import mozilla.lockbox.model.ItemViewModel

class ItemViewHolder(override val containerView: View) : ItemListCell(containerView) {
    var itemViewModel: ItemViewModel? = null
        set(value) {
            field = value
            value?.let {
                itemTitle.text = it.title
                itemSubtitle.text = textFromSubtitle(it.subtitle)
            }
        }

    private fun textFromSubtitle(subtitle: String?): CharSequence {
        return if (subtitle!!.isEmpty()) {
            containerView.resources.getString(R.string.no_username)
        } else {
            subtitle
        }
    }
}
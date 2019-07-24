/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.detail_menu.view.*
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemDetailOptionAdapter

class ItemDetailOptionMenu(
    val context: Context,
    private val listener: View.OnClickListener
) : PopupWindow(), View.OnClickListener {

    var dismissListener: (() -> Unit)? = null

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.fragment_item_detail, null)

        with(contentView.menuList) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }

        setBackgroundDrawable(context.getDrawable(R.drawable.sort_menu_bg))

        isFocusable = true

        height = ViewGroup.LayoutParams.WRAP_CONTENT
        width = ViewGroup.LayoutParams.WRAP_CONTENT

        elevation = context.resources.getDimension(R.dimen.menu_elevation)
    }

    override fun onClick(v: View?) {
        dismiss()
        listener.onClick(v)
    }

    fun show(anchor: View) {
//        val xOffset = if (ViewCompat.getLayoutDirection(anchor) == ViewCompat.LAYOUT_DIRECTION_RTL) -anchor.width else 0
//        val yOffset = anchor.height + anchor.paddingBottom

        super.showAsDropDown(anchor, 0, 0)
    }

    override fun dismiss() {
        super.dismiss()
        dismissListener?.invoke()
    }
}
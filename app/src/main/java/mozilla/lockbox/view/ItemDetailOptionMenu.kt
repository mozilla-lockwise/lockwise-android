/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.ViewCompat

class ItemDetailOptionMenu(
    val context: Context,
    val listener: View.OnClickListener
) : PopupWindow(), View.OnClickListener {

    var dismissListener: (() -> Unit)? = null

//    init {
//        contentView = LayoutInflater.from(context).inflate(R.layout.menu, null)
//
//        with(contentView.list) {
//            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//            adapter = HomeMenuAdapter(context, this@HomeMenu)
//        }
//
//        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//
//        isFocusable = true
//
//        height = ViewGroup.LayoutParams.WRAP_CONTENT
//        width = ViewGroup.LayoutParams.WRAP_CONTENT
//
//        elevation = context.resources.getDimension(R.dimen.menu_elevation)
//    }

    override fun onClick(v: View?) {
        dismiss()
        listener.onClick(v)
    }

    fun show(anchor: View) {
        val xOffset = if (ViewCompat.getLayoutDirection(anchor) == ViewCompat.LAYOUT_DIRECTION_RTL) -anchor.width else 0
        val yOffset = anchor.height + anchor.paddingBottom

        super.showAsDropDown(anchor, xOffset, yOffset)
    }

    override fun dismiss() {
        super.dismiss()
        dismissListener?.invoke()
    }
}
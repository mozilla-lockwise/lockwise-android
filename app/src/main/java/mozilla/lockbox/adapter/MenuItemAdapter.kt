/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import mozilla.lockbox.R
import mozilla.lockbox.action.Setting

@Suppress("UNCHECKED_CAST")
abstract class MenuItemAdapter<T>(
    context: Context,
    textViewResourceId: Int,
    val values: ArrayList<T>
) : ArrayAdapter<T>(context, textViewResourceId, values) {

    abstract fun setSelection(position: Int)
    open var selectedIndex = -1
    open val type = if (values[0] is Setting.ItemListSort) {
        SORT_ITEM_LIST
    } else {
        DELETE_ITEM
    }

    companion object {
        private const val SORT_ITEM_LIST = 0
        private const val DELETE_ITEM = 1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.setTextAppearance(R.style.TextAppearanceWidgetEventToolbarTitle)
        label.setTextColor(Color.WHITE)
        label.setBackgroundColor(label.resources.getColor(R.color.color_primary, null))
//        label.text = context.resources.getString(values[position].titleId)
//        label.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_down_caret, 0)

        return label
    }

    private fun <T> setItemListLabelAppearance(
        label: TextView,
        position: Int
//        ,
//        values: ArrayList<T>
    ): View {

//        if (type == SORT_ITEM_LIST) {
//            label.text = context.resources.getString(values[position].valueId)
//        }
        label.setTextAppearance(R.style.TextAppearanceSortMenuItem)

        label.setBackgroundColor(Color.WHITE)
        val padding = label.resources.getDimensionPixelSize(R.dimen.sort_item_padding)
        label.setPadding(padding, padding, padding, padding)

//        val drawable: Drawable = setDrawable(values)
//        label.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)

        if (position == selectedIndex) {
            label.setBackgroundResource(R.color.selection)
        }
        return label
    }

//    private fun setDrawable(values: ArrayList): Drawable {
//        if (values[0] is Setting.ItemListSort) {
//            R.drawable.ic_down_caret
//        } else {
//            R.drawable.ic_menu_kebab
//        }
//    }

//    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//        val label = super.getDropDownView(position, convertView, parent) as TextView
//        return if (values[0] is Setting.ItemListSort) {
//            val type = Setting.ItemListSort()
//            setItemListLabelAppearance(label, position, values as ArrayList<Setting.ItemListSort>, Setting.ItemListSort)
//        } else {
//            label
//        }
//    }
}
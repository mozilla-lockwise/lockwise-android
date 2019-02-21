package mozilla.lockbox.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.ArrayAdapter
import mozilla.lockbox.R
import mozilla.lockbox.action.Setting

class SortItemAdapter(
    context: Context,
    textViewResourceId: Int,
    val values: ArrayList<Setting.ItemListSort>
) : ArrayAdapter<Setting.ItemListSort>(context, textViewResourceId, values) {

    private var selectedIndex = -1

    fun setSelection(position: Int) {
        selectedIndex = position
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.setTextAppearance(R.style.TextAppearanceWidgetEventToolbarTitle)
        label.setTextColor(Color.WHITE)
        label.setBackgroundColor(label.resources.getColor(R.color.color_primary, null))
        label.text = context.resources.getString(values[position].titleId)
        label.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_down_caret, 0)

        return label
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getDropDownView(position, convertView, parent) as TextView
        label.setTextAppearance(R.style.TextAppearanceSortMenuItem)
        label.text = context.resources.getString(values[position].valueId)
        label.setBackgroundColor(Color.WHITE)
        val padding = label.resources.getDimensionPixelSize(R.dimen.sort_item_padding)
        label.setPadding(padding, padding, padding, padding)

        if (position == selectedIndex) {
            label.setBackgroundResource(R.color.selection)
        }

        return label
    }
}
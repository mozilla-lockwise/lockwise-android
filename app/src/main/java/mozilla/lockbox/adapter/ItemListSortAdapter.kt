/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.annotation.NonNull
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

class ItemListSortAdapter(
    @NonNull context: Context,
    @LayoutRes resource: Int,
    items: Array<String>
) : ArrayAdapter<String>(context, resource, items) {

    var selectedItemPosition: Int? = null
    var selectedBackgroundColor: Int? = null
    private val theme = context.resources.newTheme()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = super.getView(position, convertView, parent)
        val selectedColor = selectedBackgroundColor
        if (selectedItemPosition != null &&
            selectedColor != null &&
            selectedItemPosition == position) {
            view.setBackgroundColor(context.resources.getColor(selectedColor, theme))
        } else {
            view.setBackgroundColor(context.resources.getColor(android.R.color.background_light, theme))
        }
        return view
    }
}

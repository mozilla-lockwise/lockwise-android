/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mozilla.lockbox.R

class ItemDetailOptionAdapter(
    private val listener: View.OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items: List<MenuItem> = listOf(
//        MenuItem(R.id.edit, context.getString(R.string.edit)),
//        MenuItem(R.id.delete, context.getString(R.string.delete))
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return MenuItemViewHolder(view as TextView, listener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MenuItemViewHolder).bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

private class MenuItemViewHolder(
    val labelView: TextView,
    val listener: View.OnClickListener
) : RecyclerView.ViewHolder(labelView) {

    fun bind(item: MenuItem) {
        labelView.id = item.id
        labelView.text = item.label

        labelView.setOnClickListener(listener)
    }
}

private class MenuItem(
    val id: Int,
    val label: String
)
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

// Large portions of this class are borrowed from:
// https://gist.github.com/gabrielemariotti/4c189fb1124df4556058

package mozilla.lockbox.adapter

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.util.SparseArray
import android.view.View
import android.widget.LinearLayout
import mozilla.lockbox.R

class SectionedAdapter(
    private val sectionLayoutId: Int,
    private val sectionTitleId: Int,
    private val baseAdapter: Adapter<ViewHolder>
) : Adapter<ViewHolder>() {

    private var valid = true
    private val sections = SparseArray<Section>()

    companion object {
        const val SECTION_TYPE = 0
    }

    class SectionViewHolder(view: View, titleResourceId: Int) : ViewHolder(view) {
        var title: TextView = view.findViewById(titleResourceId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, typeView: Int): ViewHolder {
        return if (typeView == SECTION_TYPE) {
            val view = LayoutInflater.from(parent.context).inflate(sectionLayoutId, parent, false)
            SectionViewHolder(view, sectionTitleId)
        } else {
            baseAdapter.onCreateViewHolder(parent, typeView - 1)
        }
    }

    override fun onBindViewHolder(sectionViewHolder: ViewHolder, position: Int) {
        if (isSectionHeaderPosition(position)) {
            val title = sectionViewHolder.itemView.context.getString(sections.get(position).title)
            (sectionViewHolder as SectionViewHolder).title.text = title
            sectionViewHolder.title.contentDescription = title
            setTitleTopMargin(sectionViewHolder.title, position)
        } else {
            baseAdapter.onBindViewHolder(sectionViewHolder, sectionedPositionToPosition(position))
        }
    }

    private fun setTitleTopMargin(textView: TextView, position: Int) {
        val marginTop = if (position == 0) {
            textView.resources.getDimensionPixelSize(R.dimen.section_first_title_top_margin)
        } else {
            textView.resources.getDimensionPixelSize(R.dimen.section_title_top_margin)
        }
        (textView.layoutParams as LinearLayout.LayoutParams).setMargins(0, marginTop, 0, 0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSectionHeaderPosition(position))
            SECTION_TYPE
        else
            baseAdapter.getItemViewType(sectionedPositionToPosition(position)) + 1
    }
    class Section(internal var firstPosition: Int, @StringRes title: Int) {
        internal var sectionedPosition: Int = 0
        @StringRes var title: Int
            internal set

        init {
            this.title = title
        }
    }

    fun setSections(sections: List<Section>) {
        this.sections.clear()

        sections.sortedWith(
            Comparator { o, o1 ->
                when {
                    o.firstPosition == o1.firstPosition -> 0
                    o.firstPosition < o1.firstPosition -> -1
                    else -> 1
                }
            }
        )

        var offset = 0 // offset positions for the headers we're adding
        for (section in sections) {
            section.sectionedPosition = section.firstPosition + offset
            this.sections.append(section.sectionedPosition, section)
            ++offset
        }

        notifyDataSetChanged()
    }

    private fun sectionedPositionToPosition(sectionedPosition: Int): Int {
        if (isSectionHeaderPosition(sectionedPosition)) {
            return RecyclerView.NO_POSITION
        }

        var offset = 0
        for (i in 0 until sections.size()) {
            if (sections.valueAt(i).sectionedPosition > sectionedPosition) {
                break
            }
            --offset
        }
        return sectionedPosition + offset
    }

    fun isSectionHeaderPosition(position: Int): Boolean {
        return sections.get(position) != null
    }

    override fun getItemCount(): Int {
        return if (valid) baseAdapter.itemCount + sections.size() else 0
    }
}
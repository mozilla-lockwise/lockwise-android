/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

// Large portions of this class are borrowed from:
// https://gist.github.com/gabrielemariotti/4c189fb1124df4556058

package mozilla.lockbox.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.util.SparseArray
import android.view.View

class SectionedAdapter(
    private val sectionLayoutId: Int,
    private val sectionTitleId: Int,
    private val baseAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var valid = true
    private val sections = SparseArray<Section>()

    companion object {
        const val SECTION_TYPE = 0
    }

    class SectionViewHolder(view: View, titleResourceId: Int) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(titleResourceId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, typeView: Int): RecyclerView.ViewHolder {
        if (typeView == SECTION_TYPE) {
            val view = LayoutInflater.from(parent.context).inflate(sectionLayoutId, parent, false)
            return SectionViewHolder(view, sectionTitleId)
        } else {
            return baseAdapter.onCreateViewHolder(parent, typeView - 1)
        }
    }

    override fun onBindViewHolder(sectionViewHolder: RecyclerView.ViewHolder, position: Int) {
        if (isSectionHeaderPosition(position)) {
            (sectionViewHolder as SectionViewHolder).title.text = sections.get(position).title
        } else {
            baseAdapter.onBindViewHolder(sectionViewHolder, sectionedPositionToPosition(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSectionHeaderPosition(position))
            SECTION_TYPE
        else
            baseAdapter.getItemViewType(sectionedPositionToPosition(position)) + 1
    }

    class Section(internal var firstPosition: Int, title: CharSequence) {
        internal var sectionedPosition: Int = 0
        var title: CharSequence
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
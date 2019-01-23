/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.detaches
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.android.extensions.LayoutContainer
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.view.ItemViewHolder

open class ItemListCell(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

class ItemListAdapter : RecyclerView.Adapter<ItemListCell>() {

    private var itemList: List<ItemViewModel>? = null
    private val _clicks = PublishSubject.create<ItemViewModel>()
    private val compositeDisposable = CompositeDisposable()
    private var isFiltering: Boolean = false

    companion object {
        private const val ITEM_DISPLAY_CELL_TYPE = 0
        private const val NO_MATCHING_ENTRIES_CELL_TYPE = 1
        private const val NO_ENTRIES_CELL_TYPE = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListCell {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            NO_MATCHING_ENTRIES_CELL_TYPE -> {
                val view = inflater.inflate(R.layout.list_cell_no_matching, parent, false)

                return ItemListCell(view)
            }
            NO_ENTRIES_CELL_TYPE ->
                return ItemListCell(inflater.inflate(R.layout.list_cell_no_entries, parent, false))
            else -> {
                val view = inflater.inflate(R.layout.list_cell_item, parent, false)

                val viewHolder = ItemViewHolder(view)
                view
                    .clicks()
                    .takeUntil(parent.detaches())
                    .subscribe {
                        viewHolder.itemViewModel?.let(this._clicks::onNext)
                    }
                    .addTo(compositeDisposable)

                return viewHolder
            }
        }
    }

    override fun getItemCount(): Int {
        val list = itemList ?: return 0
        val count = list.count()
        return if (count == 0) 1 else count
    }

    override fun onBindViewHolder(holder: ItemListCell, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                val viewModel = itemList?.get(position)
                holder.itemViewModel = viewModel
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val count = itemList?.count() ?: 0
        return when (count) {
            0 -> if (isFiltering) NO_MATCHING_ENTRIES_CELL_TYPE else NO_ENTRIES_CELL_TYPE
            else -> ITEM_DISPLAY_CELL_TYPE
        }
    }

    fun updateItems(newItems: List<ItemViewModel>, isFiltering: Boolean = false) {
        this.isFiltering = isFiltering
        itemList = newItems
        // note: this is not a performant way to do updates; we should think about using
        // diffutil here when implementing filtering / sorting
        notifyDataSetChanged()
    }

    fun clicks(): Observable<ItemViewModel> {
        return this._clicks
    }
}
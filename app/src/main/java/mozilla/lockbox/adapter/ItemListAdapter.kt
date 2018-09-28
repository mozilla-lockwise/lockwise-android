/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.detaches
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.view.ItemViewHolder

class ItemListAdapter : RecyclerView.Adapter<ItemViewHolder>() {
    private var itemList: List<ItemViewModel>? = null
    private val _clicks = PublishSubject.create<ItemViewModel>()
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
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

    override fun getItemCount(): Int {
        return itemList?.count() ?: 0
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val viewModel = itemList?.get(position)
        holder.itemViewModel = viewModel
    }

    fun updateItems(newItems: List<ItemViewModel>) {
        itemList = newItems
        // note: this is not a performant way to do updates; we should think about using
        // diffutil here when implementing filtering / sorting
        notifyDataSetChanged()
    }

    fun clicks(): Observable<ItemViewModel> {
        return this._clicks
    }
}
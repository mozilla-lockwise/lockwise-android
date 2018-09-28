/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_filter.view.*
import kotlinx.android.synthetic.main.include_backable_filter.view.*
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemListAdapter
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.presenter.FilterPresenter
import mozilla.lockbox.presenter.FilterView


class FilterFragment : BackableFragment(), FilterView {
    val adapter = ItemListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        presenter = FilterPresenter(this)
        val view = inflater.inflate(R.layout.fragment_filter, container, false)
        setupBackable(view, R.drawable.ic_arrow_back)

        val layoutManager = LinearLayoutManager(context)
        view.entriesView.layoutManager = layoutManager
        view.entriesView.adapter = adapter

        return view
    }

    override fun onResume() {
        super.onResume()
        view!!.filterField.requestFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view!!.filterField, InputMethodManager.SHOW_IMPLICIT)
    }

    override val filterTextEntered: Observable<CharSequence>
        get() = view!!.filterField.textChanges()

    override val filterText: Consumer<in CharSequence>
        get() = view!!.filterField.text()

    override val cancelButtonClicks: Observable<Unit>
        get() = view!!.cancelButton.clicks()

    override val cancelButtonVisibility: Consumer<in Boolean>
        get() = view!!.cancelButton.visibility()

    override fun updateItems(items: List<ItemViewModel>) {
        adapter.updateItems(items)
    }
}

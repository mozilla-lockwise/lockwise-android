/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_filter.view.*
import kotlinx.android.synthetic.main.include_backable_filter.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemListAdapter
import mozilla.lockbox.adapter.ItemListAdapterType
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.presenter.AppFilterPresenter
import mozilla.lockbox.presenter.FilterView

@ExperimentalCoroutinesApi
class FilterFragment : BackableFragment(), FilterView {

    val adapter = ItemListAdapter(ItemListAdapterType.Filter)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = AppFilterPresenter(this)
        val view = inflater.inflate(R.layout.fragment_filter, container, false)

        val layoutManager = LinearLayoutManager(context)
        view.entriesView.layoutManager = layoutManager
        view.entriesView.adapter = adapter
        val decoration = DividerItemDecoration(context, layoutManager.orientation)
        val decorationDrawable = context?.getDrawable(R.drawable.inset_divider)
        decorationDrawable?.let { decoration.setDrawable(it) }
        view.entriesView.addItemDecoration(decoration)

        return view
    }

    override fun onResume() {
        super.onResume()
        requireView().filterField.requestFocus()
        openKeyboard(view?.filterField)
    }

    override fun onPause() {
        super.onPause()
        closeKeyboard(view?.filterField)
    }

    override val filterTextEntered: Observable<CharSequence>
        get() = requireView().filterField.textChanges()

    override val filterText: Consumer<in CharSequence>
        get() = Consumer { newText -> requireView().filterField.setText(newText) }

    override val cancelButtonClicks: Observable<Unit>
        get() = requireView().cancelButton.clicks()

    override val cancelButtonVisibility: Consumer<in Boolean>
        get() = requireView().cancelButton.visibility()

    override val itemSelection: Observable<ItemViewModel>
        get() = adapter.itemClicks

    override val onDismiss: Observable<Unit>? = null

    override val displayNoEntries: ((Boolean) -> Unit)? = null

    override fun updateItems(items: List<ItemViewModel>) {
        adapter.updateItems(items)
    }
}

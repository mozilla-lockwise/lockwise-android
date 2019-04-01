/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_autofill_filter.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ItemListAdapter
import mozilla.lockbox.adapter.ItemListAdapterType
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.presenter.AutofillFilterPresenter
import mozilla.lockbox.presenter.AutofillFilterView

@ExperimentalCoroutinesApi
class AutofillFilterFragment : DialogFragment(), AutofillFilterView {
    val adapter = ItemListAdapter(ItemListAdapterType.AutofillFilter)

    override val onDismiss: Observable<Unit> = PublishSubject.create<Unit>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = AutofillFilterPresenter(this)
        val view = inflater.inflate(R.layout.fragment_autofill_filter, container, false)

        val layoutManager = LinearLayoutManager(context)
        view.entriesView.layoutManager = layoutManager
        view.entriesView.adapter = adapter
        retainInstance = true
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.NoTitleDialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        super.onViewCreated(view, savedInstanceState)
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
    override val itemSelection: Observable<ItemViewModel>
        get() = adapter.itemClicks

    override fun updateItems(items: List<ItemViewModel>) {
        adapter.updateItems(items)
    }

    override fun displayNoEntries(enabled: Boolean) {
        adapter.displayNoEntries(enabled)
    }

    override fun onCancel(dialog: DialogInterface) {
        (onDismiss as PublishSubject).onNext(Unit)
        super.onCancel(dialog)
    }

    override fun onDestroyView() {
        val dialog = dialog
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }

        super.onDestroyView()
    }
}
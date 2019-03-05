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
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_autofill_filter.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.AutofillFilterPresenter
import mozilla.lockbox.presenter.AutofillFilterView

class AutofillFilterFragment : DialogFragment(), AutofillFilterView {
    override val onDismiss: Observable<Unit> = PublishSubject.create<Unit>()
    private var isEnablingDismissed: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = AutofillFilterPresenter(this)
        retainInstance = true
        return inflater.inflate(R.layout.fragment_autofill_filter, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.NoTitleDialog)
    }

    override val fillMeButtonClicks: Observable<Unit>
        get() = view!!.fillMePlaceholder.clicks()

//    override val filterTextEntered: Observable<CharSequence>
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//    override val filterText: Consumer<in CharSequence>
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//    override val cancelButtonClicks: Observable<Unit>
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//    override val cancelButtonVisibility: Consumer<in Boolean>
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//    override val itemSelection: Observable<ItemViewModel>
//        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
//
//    override fun updateItems(items: List<ItemViewModel>) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override fun onDestroyView() {
        if (isEnablingDismissed) {
            (onDismiss as PublishSubject).onNext(Unit)
        }

        val dialog = dialog
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }

        super.onDestroyView()
    }
}
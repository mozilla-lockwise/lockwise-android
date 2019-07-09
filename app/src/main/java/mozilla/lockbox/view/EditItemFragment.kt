/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.inputHostname
import kotlinx.android.synthetic.main.fragment_item_detail.inputLayoutHostname
import kotlinx.android.synthetic.main.fragment_item_detail.inputLayoutPassword
import kotlinx.android.synthetic.main.fragment_item_detail.inputLayoutUsername
import kotlinx.android.synthetic.main.fragment_item_detail.inputPassword
import kotlinx.android.synthetic.main.fragment_item_detail.inputUsername
import kotlinx.android.synthetic.main.fragment_item_detail.toolbar
import kotlinx.android.synthetic.main.fragment_item_detail.view.entryTitle
import kotlinx.android.synthetic.main.fragment_item_edit.*
import kotlinx.android.synthetic.main.fragment_item_edit.view.*
import kotlinx.android.synthetic.main.fragment_item_edit.view.inputLayoutName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.EditItemDetailView
import mozilla.lockbox.presenter.EditItemPresenter
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class EditItemFragment : BackableFragment(), EditItemDetailView {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemId = arguments?.let {
            ItemDetailFragmentArgs.fromBundle(it)
                .itemId
        }

        presenter = EditItemPresenter(this, itemId)
        return inflater.inflate(R.layout.fragment_item_edit, container, false)
    }

    override val deleteClicks: Observable<Unit>
        get() = view!!.deleteEntryButton.clicks()

    override val closeEntryClicks: Observable<Unit>
        get() = view!!.closeEntryButton.clicks()

    override val saveEntryClicks: Observable<Unit>
        get() = view!!.saveEntryButton.clicks()

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
        toolbar.title = item.title
        toolbar.entryTitle.text = item.title
        toolbar.entryTitle.gravity = Gravity.CENTER_VERTICAL

        inputLayoutName.isHintAnimationEnabled = false
        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false

        inputName.readOnly = true

        inputHostname.isFocusable = true
        inputHostname.isClickable = true

        inputUsername.isFocusable = true
        inputUsername.isClickable = true

        inputPassword.isFocusable = true
        inputPassword.isClickable = true

        inputName.setText(item.hostname, TextView.BufferType.NORMAL)
        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)

        if (!item.hasUsername) {
            inputUsername.setText(R.string.empty_space, TextView.BufferType.NORMAL)
        } else {
            inputUsername.setText(item.username, TextView.BufferType.NORMAL)
        }
    }
}
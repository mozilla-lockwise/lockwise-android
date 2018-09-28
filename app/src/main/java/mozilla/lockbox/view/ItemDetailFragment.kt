/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_item_detail.*
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.presenter.ItemDetailPresenter
import mozilla.lockbox.presenter.ItemDetailView

class ItemDetailFragment : BackableFragment(), ItemDetailView {
    override var itemId: String? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        presenter = ItemDetailPresenter(this)
        val view = inflater.inflate(R.layout.fragment_item_detail, container, false)
        setupBackable(view)

        return view
    }

    override fun updateItem(item: ItemViewModel) {
        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false

        inputUsername.readOnly = true
        inputPassword.readOnly = true
        inputHostname.readOnly = true

        inputHostname.setText(item.title, TextView.BufferType.NORMAL)
        inputUsername.setText(item.subtitle, TextView.BufferType.NORMAL)
        inputPassword.setText(item.guid, TextView.BufferType.NORMAL)
    }
}

var EditText.readOnly: Boolean
    get() = this.isFocusable
    set(readOnly) {
            this.isFocusable = !readOnly
            this.isFocusableInTouchMode = !readOnly
            this.isClickable = !readOnly
            this.isLongClickable = !readOnly
            this.isCursorVisible = !readOnly
        }

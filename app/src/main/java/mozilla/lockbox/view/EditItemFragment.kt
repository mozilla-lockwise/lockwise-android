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
import android.view.WindowManager
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_edit_item.*
import kotlinx.android.synthetic.main.fragment_edit_item.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.EditItemView
import mozilla.lockbox.presenter.EditItemPresenter
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class EditItemFragment : ItemMutationFragment(), EditItemView {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemId = arguments?.let {
            EditItemFragmentArgs.fromBundle(it).itemId
        }

        presenter = EditItemPresenter(this, itemId)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return inflater.inflate(R.layout.fragment_edit_item, container, false)
    }

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()

        toolbar.title = item.title

        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)

        if (!item.hasUsername) {
            inputUsername.setText(R.string.empty_string, TextView.BufferType.NORMAL)
        } else {
            inputUsername.setText(item.username, TextView.BufferType.NORMAL)
        }
    }

    override fun setupKeyboardFocus(view: View) {
        super.setupKeyboardFocus(view)

        view.fragment_edit_item.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                closeKeyboard()
            }
        }

        view.fragment_edit_item.setOnTouchListener { _, _ ->
            closeKeyboard()
            view.clearFocus()
            true
        }
    }

    override fun setupItemDisplay(view: View) {
        super.setupItemDisplay(view)

        view.toolbar.toolbarTitle.text = getString(R.string.edit_login_title)
        view.inputHostname.readOnly = true
    }
}

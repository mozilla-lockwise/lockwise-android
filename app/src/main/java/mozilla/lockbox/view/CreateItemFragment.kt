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
import kotlinx.android.synthetic.main.fragment_create_item.*
import kotlinx.android.synthetic.main.fragment_create_item.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.presenter.CreateItemPresenter
import mozilla.lockbox.presenter.CreateItemView

@ExperimentalCoroutinesApi
class CreateItemFragment : ItemMutationFragment(), CreateItemView {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = CreateItemPresenter(this)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return inflater.inflate(R.layout.fragment_create_item, container, false)
    }

    override fun setupKeyboardFocus(view: View) {
        super.setupKeyboardFocus(view)

        view.fragment_create_item.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
            }
        }

        view.fragment_create_item.setOnTouchListener { _, _ ->
            closeKeyboard()
            view.clearFocus()
            true
        }
    }

    override fun setupItemDisplay(view: View) {
        super.setupItemDisplay(view)
        view.toolbarTitle.text = getString(R.string.create_title)

        // We have to set the hint manually here because we want to disable the hint becoming
        // the title, which is what happens if we do it in the XML.
        view.inputHostname.hint = getString(R.string.create_hostname_hint_text)
        view.inputUsername.hint = getString(R.string.create_username_hint_text)
        view.inputPassword.hint = getString(R.string.hint_password)
    }

    override fun getToastAction(strId: Int?): RouteAction {
        return RouteAction.ShowToastNotification(strId = strId, viewGroup = this.view as ViewGroup)
    }
}
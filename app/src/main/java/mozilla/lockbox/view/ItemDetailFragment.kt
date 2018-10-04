/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.view.*
import kotlinx.android.synthetic.main.include_backable.*
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.ItemDetailPresenter
import mozilla.lockbox.presenter.ItemDetailView

class ItemDetailFragment : BackableFragment(), ItemDetailView {
    override var itemId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = ItemDetailPresenter(this, activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        val view = inflater.inflate(R.layout.fragment_item_detail, container, false)
        setupBackable(view)

        return view
    }

    override val btnUsernameCopyClicks: Observable<Unit>
        get() = view!!.btnUsernameCopy.clicks()

    override val btnPasswordCopyClicks: Observable<Unit>
        get() = view!!.btnPasswordCopy.clicks()

    override val btnTogglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override fun updateItem(item: ItemDetailViewModel) {
        toolbar.title = item.title

        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false

        inputUsername.readOnly = true
        inputPassword.readOnly = true
        inputHostname.readOnly = true

        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputUsername.setText(item.username, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)
    }

    override fun copyNotification(@StringRes strId: Int) {
        Toast.makeText(activity, getString(strId), Toast.LENGTH_SHORT).show()
    }

    override fun updatePasswordField(visible: Boolean) {

        if (visible) {
            inputPassword.transformationMethod = null
            btnPasswordToggle.setImageResource(R.drawable.ic_hide)
        } else {
            inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnPasswordToggle.setImageResource(R.drawable.ic_show)
        }
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

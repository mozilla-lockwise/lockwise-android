/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.text.InputType
import androidx.annotation.StringRes
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
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
import kotlinx.android.synthetic.main.fragment_warning.view.*
import kotlinx.android.synthetic.main.include_backable.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.ItemDetailPresenter
import mozilla.lockbox.presenter.ItemDetailView
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class ItemDetailFragment : BackableFragment(), ItemDetailView {

    private val errorHelper = NetworkErrorHelper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemId = arguments?.let {
            ItemDetailFragmentArgs.fromBundle(it)
                .itemId
        }

        presenter = ItemDetailPresenter(this, itemId)
        return inflater.inflate(R.layout.fragment_item_detail, container, false)
    }

    override val usernameCopyClicks: Observable<Unit>
        get() = view!!.inputUsername.clicks()

    override val passwordCopyClicks: Observable<Unit>
        get() = view!!.inputPassword.clicks()

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override val hostnameClicks: Observable<Unit>
        get() = view!!.inputHostname.clicks()

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            if (value) {
                inputPassword.transformationMethod = null
                btnPasswordToggle.setImageResource(R.drawable.ic_hide)
            } else {
                inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnPasswordToggle.setImageResource(R.drawable.ic_show)
            }
        }

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.title = item.title

        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false

        inputUsername.readOnly = true
        inputPassword.readOnly = true
        inputHostname.readOnly = true
        btnHostnameLaunch.isClickable = false

        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputUsername.setText(item.username, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)
    }

    override fun showToastNotification(@StringRes strId: Int) {
        assertOnUiThread()
        val toast = Toast(activity)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layoutInflater.inflate(R.layout.toast_view, this.view as ViewGroup, false)
        toast.setGravity(Gravity.FILL_HORIZONTAL or Gravity.BOTTOM, 0, 0)
        val v = toast.view.findViewById(R.id.message) as TextView
        v.text = resources.getString(strId)
        toast.show()
    }

    override fun handleNetworkError(networkErrorVisibility: Boolean) {
        if (!networkErrorVisibility) {
            errorHelper.showNetworkError(view!!)
        } else {
            errorHelper.hideNetworkError(view!!, view!!.card_view, R.dimen.hidden_network_error)
        }
    }

    override val retryNetworkConnectionClicks: Observable<Unit>
        get() = view!!.networkWarning.retryButton.clicks()
}

var EditText.readOnly: Boolean
    get() = this.isFocusable
    set(readOnly) {
        this.isFocusable = true
        this.isFocusableInTouchMode = !readOnly
        this.inputType = InputType.TYPE_NULL
        this.isClickable = true
        this.isLongClickable = !readOnly
        this.isCursorVisible = !readOnly
    }

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.Setting
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.ItemDetailPresenter
import mozilla.lockbox.presenter.ItemDetailView
import mozilla.lockbox.support.assertOnUiThread

@Suppress("DEPRECATION")
@ExperimentalCoroutinesApi
class ItemDetailFragment : BackableFragment(), ItemDetailView {
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

    private val errorHelper = NetworkErrorHelper()

    override val usernameCopyClicks: Observable<Unit>
        get() = view!!.inputUsername.clicks()

    override val passwordCopyClicks: Observable<Unit>
        get() = view!!.inputPassword.clicks()

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override val hostnameClicks: Observable<Unit>
        get() = view!!.inputHostname.clicks()

    override val learnMoreClicks: Observable<Unit>
        get() = view!!.detailLearnMore.clicks()

    override val kebabMenuClicks: Observable<Unit>
        get() = view!!.toolbar.kebabMenu.clicks()

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    private fun updatePasswordVisibility(visible: Boolean) {
        if (visible) {
            inputPassword.transformationMethod = null
            btnPasswordToggle.setImageResource(R.drawable.ic_hide)
        } else {
            inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnPasswordToggle.setImageResource(R.drawable.ic_show)
        }
    }

    override fun updateKebabMenu() {
//        sortItemsAdapter.setSelection(sortMenuOptions.indexOf(sort))
        spinner.setSelection(0, false)
//        scrollToTop()
    }

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
        toolbar.title = item.title
//        val menuIcon = resources.getDrawable(R.drawable.ic_menu_kebab)
//        toolbar.kebabMenu.

        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false

        inputUsername.readOnly = true

        if (!item.hasUsername) {
            btnUsernameCopy.setColorFilter(resources.getColor(R.color.white_60_percent, null))
            inputUsername.isClickable = false
            inputUsername.isFocusable = false
            inputUsername.setText(R.string.empty_space, TextView.BufferType.NORMAL)
        } else {
            btnUsernameCopy.clearColorFilter()
            inputUsername.isClickable = true
            inputUsername.isFocusable = true
            inputUsername.setText(item.username, TextView.BufferType.NORMAL)
        }

        inputPassword.readOnly = true
        inputPassword.isClickable = true
        inputPassword.isFocusable = true

        inputHostname.readOnly = true
        inputHostname.isClickable = true
        inputHostname.isFocusable = true

        btnHostnameLaunch.isClickable = false

        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)

        // effect password visibility state
        updatePasswordVisibility(isPasswordVisible)
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
            errorHelper.hideNetworkError(view!!, view!!.cardView, R.dimen.hidden_network_error)
        }
    }

//    override val retryNetworkConnectionClicks: Observable<Unit>
//        get() = view!!.networkWarning.retryButton.clicks()
}

var EditText.readOnly: Boolean
    get() = this.isFocusable
    set(readOnly) {
        this.isFocusable = !readOnly
        this.isFocusableInTouchMode = !readOnly
        this.isClickable = !readOnly
        this.isLongClickable = !readOnly
        this.isCursorVisible = !readOnly
        this.inputType = InputType.TYPE_NULL
    }
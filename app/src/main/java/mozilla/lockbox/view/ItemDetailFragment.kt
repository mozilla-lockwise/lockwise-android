/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_item_detail.*
import kotlinx.android.synthetic.main.fragment_item_detail.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.ItemDetailPresenter
import mozilla.lockbox.presenter.ItemDetailView
import mozilla.lockbox.support.assertOnUiThread
import androidx.appcompat.view.menu.MenuBuilder

@ExperimentalCoroutinesApi
class ItemDetailFragment : BackableFragment(), ItemDetailView {

    private var itemId: String? = null
    private var kebabMenu: ItemDetailOptionMenu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        itemId = arguments?.let {
            ItemDetailFragmentArgs.fromBundle(it).itemId
        }

        this.setHasOptionsMenu(true)
        presenter = ItemDetailPresenter(this, itemId)

        return inflater.inflate(R.layout.fragment_item_detail, container, false)
    }

    override fun onPause() {
        kebabMenu?.dismiss()
        super.onPause()
    }

    override fun onDestroy() {
        kebabMenu?.dismiss()
        super.onDestroy()
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

    override val kebabMenuClicks: Observable<Unit>
        get() = view!!.toolbar.kebabMenuButton.clicks()

    override val editClicks: BehaviorRelay<Unit> = BehaviorRelay.create()

    override val deleteClicks: BehaviorRelay<Unit> = BehaviorRelay.create()

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    override fun showPopup() {
        val wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
        val popupMenu = PopupMenu(wrapper, this.kebabMenuButton)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.edit -> {
                    editClicks.accept(Unit)
                    true
                }
                R.id.delete -> {
                    deleteClicks.accept(Unit)
                    true
                }
                else -> false
            }
        }

        popupMenu.inflate(R.menu.item_detail_menu)

        val builder = popupMenu.menu as MenuBuilder
        builder.setOptionalIconsVisible(true)
        popupMenu.show()
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

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
        toolbar.title = item.title
        toolbar.entryTitle.text = item.title
        toolbar.entryTitle.gravity = Gravity.CENTER_VERTICAL

        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false

        inputPassword.ellipsize = TextUtils.TruncateAt.END
        inputPassword.setSingleLine()
        inputPassword.maxLines = 1

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

    // used for feature flag
    override fun showKebabMenu() {
        toolbar.kebabMenuButton.visibility = View.VISIBLE
        toolbar.kebabMenuButton.isClickable = true
    }

    // used for feature flag
    override fun hideKebabMenu() {
        toolbar.kebabMenuButton.visibility = View.GONE
        toolbar.kebabMenuButton.isClickable = false
    }

    override fun handleNetworkError(networkErrorVisibility: Boolean) {
        if (!networkErrorVisibility) {
            errorHelper.showNetworkError(view!!)
        } else {
            errorHelper.hideNetworkError(view!!, view!!.cardView, R.dimen.hidden_network_error)
        }
    }
}

//    override val retryNetworkConnectionClicks: Observable<Unit>
//        get() = view!!.networkWarning.retryButton.clicks()

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
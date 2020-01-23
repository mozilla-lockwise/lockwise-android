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
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_display_item.*
import kotlinx.android.synthetic.main.fragment_display_item.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.DisplayItemPresenter
import mozilla.lockbox.presenter.DisplayItemView
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class DisplayItemFragment : BackableFragment(), DisplayItemView {

    private var itemId: String? = null
    private var kebabMenu: ItemDetailOptionMenu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        itemId = arguments?.let {
            DisplayItemFragmentArgs.fromBundle(it).itemId
        }

        this.setHasOptionsMenu(true)
        presenter = DisplayItemPresenter(this, itemId)

        return inflater.inflate(R.layout.fragment_display_item, container, false)
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
        get() = view!!.btnUsernameCopy.clicks()

    override val usernameFieldClicks: Observable<Unit>
        get() = view!!.inputUsername.clicks()

    override val passwordCopyClicks: Observable<Unit>
        get() = view!!.btnPasswordCopy.clicks()

    override val passwordFieldClicks: Observable<Unit>
        get() = view!!.inputPassword.clicks()

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override val hostnameClicks: Observable<Unit>
        get() = view!!.inputHostname.clicks()

    override val launchButtonClicks: Observable<Unit>
        get() = view!!.btnHostnameLaunch.clicks()

    override val kebabMenuClicks: Observable<Unit>
        get() = view!!.toolbar.kebabMenuButton.clicks()

    override val editClicks: Observable<Unit> = PublishSubject.create()
    override val deleteClicks: Observable<Unit> = PublishSubject.create()

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    override fun showPopup() {
        val wrapper = ContextThemeWrapper(context, R.style.PopupKebabMenu)

        val popupMenu = PopupMenu(
            wrapper,
            this.kebabMenuButton,
            Gravity.END,
            R.attr.popupWindowStyle,
            R.style.PopupKebabMenu
        )

        popupMenu.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.edit -> {
                    (editClicks as PublishSubject).onNext(Unit)
                    true
                }
                R.id.delete -> {
                    (deleteClicks as PublishSubject).onNext(Unit)
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
        toolbar.contentInsetStartWithNavigation = 0

        inputLayoutPassword.setHintTextAppearance(R.style.PasswordHint)

        inputLayoutHostname.isHintAnimationEnabled = false
        inputLayoutUsername.isHintAnimationEnabled = false
        inputLayoutPassword.isHintAnimationEnabled = false
        inputLayoutUsername.isScrollContainer = false
        view?.isScrollContainer = false

        inputUsername.readOnly = true
        inputLayoutUsername.editText?.ellipsize = TextUtils.TruncateAt.END
        inputUsername.ellipsize = TextUtils.TruncateAt.END
        inputUsername.setSingleLine()

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

        btnHostnameLaunch.isClickable = true

        inputHostname.setText(item.hostname, TextView.BufferType.NORMAL)
        inputPassword.setText(item.password, TextView.BufferType.NORMAL)

        // effect password visibility state
        updatePasswordVisibility(isPasswordVisible)
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
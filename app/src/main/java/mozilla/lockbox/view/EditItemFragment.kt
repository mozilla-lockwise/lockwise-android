/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_item_edit.*
import kotlinx.android.synthetic.main.fragment_item_edit.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.EditItemDetailView
import mozilla.lockbox.presenter.EditItemPresenter
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class EditItemFragment : BackableFragment(), EditItemDetailView {

    private val dispatcher: Dispatcher = Dispatcher.shared

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view.toolbar)
        setTextWatcher(view)
        setUpKeyboardFocus(view)
    }

    private fun setUpKeyboardFocus(view: View) {
        view.inputHostname.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
            }
        }

        view.inputUsername.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
            }
        }

        view.inputPassword.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
                dispatcher.dispatch(ItemDetailAction.TogglePassword(displayed = false))
            } else {
                // show the password when it is focused
                dispatcher.dispatch(ItemDetailAction.TogglePassword(displayed = true))
            }
        }
    }

    private fun setTextWatcher(view: View) {
        view.inputHostname.addTextChangedListener(
            buildTextWatcher(
                view.inputLayoutHostname
            )
        )
        view.inputUsername.addTextChangedListener(
            buildTextWatcher(
                view.inputLayoutUsername
            )
        )
        view.inputPassword.addTextChangedListener(
            buildTextWatcher(
                view.inputLayoutPassword
            )
        )
    }

    private fun buildTextWatcher(errorLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable) {
                val inputText: String? = errorLayout.editText?.text.toString()

                when (errorLayout.id) {
                    R.id.inputLayoutHostname -> {
                        // hostname cannot be null
                        // has to have http:// or https://
                        when {
                            TextUtils.isEmpty(inputText)
                                || (!URLUtil.isHttpUrl(inputText) and !URLUtil.isHttpsUrl(inputText))
                            -> {
                                errorLayout.setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
                                errorLayout.error = context?.getString(R.string.hostname_invalid_text)
                                errorLayout.setErrorIconDrawable(R.drawable.ic_error)
                            }
                            else -> {
                                errorLayout.error = null
                            }
                        }
                    }
                    R.id.inputLayoutUsername -> {
                        when {
                            TextUtils.isEmpty(inputText) -> {
                                errorLayout.error = null
                            }
                        }
                    }
                    R.id.inputLayoutPassword -> {
                        // password cannot be empty
                        when {
                            TextUtils.isEmpty(inputText) -> {
                                errorLayout.setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
                                errorLayout.error = context?.getString(R.string.password_invalid_text)
                                errorLayout.setErrorIconDrawable(R.drawable.ic_error)
                            }
                            else -> {
                                errorLayout.error = null
                            }
                        }
                    }
                    else -> {
                        errorLayout.error = null
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        closeKeyboard()
    }

    override fun onPause() {
        super.onPause()
        closeKeyboard()
    }

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override val learnMoreClicks: Observable<Unit>
        get() = view!!.learnMore.clicks()

    override val deleteClicks: Observable<Unit>
        get() = view!!.deleteEntryButton.clicks()

    override val closeEntryClicks: Observable<Unit>
        get() = view!!.toolbar.navigationClicks()

    override val saveEntryClicks: Observable<Unit>
        get() = view!!.saveEntryButton.clicks()

    override val hostnameChanged: Observable<CharSequence>
        get() = view!!.inputHostname.textChanges()

    override val usernameChanged: Observable<CharSequence>
        get() = view!!.inputUsername.textChanges()

    override val passwordChanged: Observable<CharSequence>
        get() = view!!.inputPassword.textChanges()

    override fun closeKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(view!!.windowToken, 0)
        }
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

    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_close, null)
        toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)
        toolbar.contentInsetStartWithNavigation = 0
    }

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
        toolbar.title = item.title
        toolbar.editLoginTitle.gravity = Gravity.CENTER_VERTICAL

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
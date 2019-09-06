/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_item_edit.*
import kotlinx.android.synthetic.main.fragment_item_edit.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.EditItemDetailView
import mozilla.lockbox.presenter.EditItemPresenter
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.assertOnUiThread
import mozilla.lockbox.support.validateEditTextAndShowError

@ExperimentalCoroutinesApi
class EditItemFragment : BackableFragment(), EditItemDetailView {

    override val togglePasswordVisibility: BehaviorRelay<Unit> = BehaviorRelay.create()

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

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
        setKeyboardFocus(view)

        val layouts = arrayOf(
            view.inputLayoutUsername,
            view.inputLayoutPassword
        )

        for (layout in layouts) {
            layout.hintTextColor = context?.getColorStateList(R.color.hint_edit_text)
            layout.setHintTextAppearance(R.style.HintText)
        }
    }

    private fun setKeyboardFocus(view: View) {
        val focusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
            }
        }

        view.inputUsername.onFocusChangeListener = focusChangeListener

        view.inputPassword.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            togglePasswordVisibility.accept(Unit)
            if (!hasFocus) {
                closeKeyboard()
            }
        }
    }

    private fun setTextWatcher(view: View) {
        val textInputs = listOf<Pair<TextInputEditText, TextInputLayout>>(
            Pair(view.inputUsername, view.inputLayoutUsername),
            Pair(view.inputPassword, view.inputLayoutPassword)
        )

        for (input in textInputs) {
            input.first.addTextChangedListener(
                buildTextWatcher(input.second)
            )
        }
    }

    private fun buildTextWatcher(errorLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                // NOOP
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                // NOOP
            }

            override fun afterTextChanged(editable: Editable) {
                val inputText: String? = errorLayout.editText?.text.toString()

                when (errorLayout.id) {
                    R.id.inputLayoutUsername -> {
                        handleUsernameChanges(errorLayout, inputText)
                    }
                    R.id.inputLayoutPassword -> {
                        handlePasswordChanges(errorLayout, inputText)
                    }
                    else -> {
                        errorLayout.error = null
                    }
                }
            }
        }
    }

    private fun handlePasswordChanges(errorLayout: TextInputLayout, inputText: String?) {
        // password cannot be empty
        when {
            TextUtils.isEmpty(inputText) -> {
                errorLayout.setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
                errorLayout.error =
                    context?.getString(R.string.password_invalid_text)
                errorLayout.setErrorIconDrawable(R.drawable.ic_error)
                view?.btnPasswordToggle?.visibility = View.INVISIBLE
            }
            else -> {
                errorLayout.error = null
                errorLayout.errorIconDrawable = null
                view?.btnPasswordToggle?.visibility = View.VISIBLE
            }
        }
    }

    private fun handleUsernameChanges(errorLayout: TextInputLayout, inputText: String?) {
        when {
            TextUtils.isEmpty(inputText) -> {
                errorLayout.error = null
            }
        }
    }

    // Removed the ability to edit the entry hostname as part of
    // https://github.com/mozilla-lockwise/lockwise-android/issues/956.
    // TODO: revisit this logic as part of https://github.com/mozilla-lockwise/lockwise-android/issues/948.
    /*
    private fun handleHostnameChanges(errorLayout: TextInputLayout, inputText: String?) {
        // hostname cannot be empty
        // has to have http:// or https://
        when {
            TextUtils.isEmpty(inputText) -> {
                errorLayout.setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
                errorLayout.error =
                    context?.getString(R.string.hostname_empty_text)
                errorLayout.setErrorIconDrawable(R.drawable.ic_error)
                view?.inputHostnameDescription?.visibility = View.INVISIBLE
            }
            !URLUtil.isHttpUrl(inputText) and !URLUtil.isHttpsUrl(inputText) -> {
                errorLayout.setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
                errorLayout.error =
                    context?.getString(R.string.hostname_invalid_text)
                errorLayout.setErrorIconDrawable(R.drawable.ic_error)
                view?.inputHostnameDescription?.visibility = View.INVISIBLE
            }
            else -> {
                errorLayout.error = null
                errorLayout.errorIconDrawable = null
                view?.inputHostnameDescription?.visibility = View.VISIBLE
            }
        }
    } */

    private fun setTextWatcher(view: View) {
        view.inputHostname.addTextChangedListener(
            buildTextWatcher(
                view.inputLayoutHostname
            )
        )
        view.inputPassword.addTextChangedListener(
            buildTextWatcher(
                view.inputLayoutHostname
            )
        )
    }

    private fun buildTextWatcher(errorLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
                errorLayout.error = null
            }

            override fun afterTextChanged(editable: Editable) {
                validateEditTextAndShowError(errorLayout)
                errorLayout.setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
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

    override fun closeKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(view!!.windowToken, 0)
        }
    }

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
        inputHostname.readOnly = true

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
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_create.*
import kotlinx.android.synthetic.main.fragment_create.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.model.titleFromHostname
import mozilla.lockbox.presenter.CreatePresenter
import mozilla.lockbox.presenter.CreateView
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class CreateFragment : BackableFragment(), CreateView {

    override val togglePasswordVisibility: BehaviorRelay<Unit> = BehaviorRelay.create()

    override val togglePasswordClicks: Observable<Unit>
        get() = view!!.btnPasswordToggle.clicks()

    override val closeEntryClicks: Observable<Unit>
        get() = view!!.toolbar.navigationClicks()

    override val saveEntryClicks: Observable<Unit>
        get() = view!!.saveEntryButton.clicks()

    override val hostnameChanged: Observable<String>
        get() = view!!.inputHostname.textChanges().map { it.toString() }

    override val usernameChanged: Observable<String>
        get() = view!!.inputUsername.textChanges().map { it.toString() }

    override val passwordChanged: Observable<String>
        get() = view!!.inputPassword.textChanges().map { it.toString() }

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    private fun updatePasswordVisibility(visible: Boolean) {
        view?.inputPassword?.setSelection(view?.inputPassword?.length() ?: 0)

        if (visible) {
            inputPassword.transformationMethod = null
            btnPasswordToggle.setImageResource(R.drawable.ic_hide)
        } else {
            inputPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnPasswordToggle.setImageResource(R.drawable.ic_show)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = CreatePresenter(this)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return inflater.inflate(R.layout.fragment_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view.toolbar)
        setupKeyboardFocus(view)
        setupItemDisplay()
    }

    private fun setupItemDisplay() {
        val layouts = arrayOf(
            view?.inputLayoutHostname,
            view?.inputLayoutUsername,
            view?.inputLayoutPassword
        )

        for (layout in layouts) {
            layout?.hintTextColor = context?.getColorStateList(R.color.hint_edit_text)
            layout?.setHintTextAppearance(R.style.HintText)
            layout?.isHintAnimationEnabled = false
            layout?.isFocusable = true
            layout?.isClickable = true
        }

        view?.apply {
            toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
            toolbar.createEntryTitle.gravity = Gravity.CENTER_VERTICAL
            hostnameDescription.text = resources.getString(R.string.create_hostname_description)

            hostnameTitle.text = resources.getString(R.string.hint_hostname)
            usernameTitle.text = resources.getString(R.string.hint_username)
            passwordTitle.text = resources.getString(R.string.hint_password)

            inputHostname.setHint(R.string.create_hostname_hint_text)
            inputUsername.setHint(R.string.create_username_hint_text)
        }
    }

    private fun setupKeyboardFocus(view: View) {
        view.inputHostname.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                view.hostnameTitle.setTextColor(resources.getColor(R.color.black_60_percent, null))
                closeKeyboard()
                if (view.inputHostname.text == null) {
                    view.inputHostname.setText(R.string.empty_space)
                }
            } else {
                view.hostnameTitle.setTextColor(resources.getColor(R.color.violet_70, null))
                val emptySpace: CharSequence = " "
                if (view.inputHostname.text == null || view.inputHostname.text == emptySpace) {
                    view.inputHostname.setText(R.string.empty_string)
                }
                view.inputHostname?.setSelection(view.inputHostname?.length() ?: 0)
            }
        }

        view.inputUsername.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                view.usernameTitle.setTextColor(resources.getColor(R.color.black_60_percent, null))
                closeKeyboard()
            } else {
                view.usernameTitle.setTextColor(resources.getColor(R.color.violet_70, null))
                view.inputUsername?.setSelection(view.inputUsername?.length() ?: 0)
            }
        }

        view.inputPassword.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            togglePasswordVisibility.accept(Unit)
            if (!hasFocus) {
                view.passwordTitle.setTextColor(resources.getColor(R.color.black_60_percent, null))
                closeKeyboard()
            } else {
                view.passwordTitle.setTextColor(resources.getColor(R.color.violet_70, null))
                view.inputPassword?.setSelection(view.inputPassword?.length() ?: 0)
            }
        }

        view.fragment_item_create.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
            }
        }

        view.fragment_item_create.setOnTouchListener { _, _ ->
            closeKeyboard()
            view.clearFocus()
            true
        }
    }

    override fun setSaveEnabled(enabled: Boolean) {
        val colorRes = if (enabled) {
            R.color.background_white
        } else {
            R.color.button_disabled
        }
        saveEntryButton.compoundDrawableTintList = context?.getColorStateList(colorRes)
        saveEntryButton.isClickable = enabled
        saveEntryButton.isFocusable = enabled
    }

    override fun displayHostnameError(@StringRes errorMessage: Int?) {
        view?.apply {
            displayError(inputLayoutHostname, errorMessage)
        }
    }

    override fun displayUsernameError(@StringRes errorMessage: Int?) {
        view?.apply {
            displayError(inputLayoutUsername, errorMessage)
        }
    }

    override fun displayPasswordError(@StringRes errorMessage: Int?) {
        view?.apply {
            displayError(inputLayoutPassword, errorMessage)
            btnPasswordToggle?.visibility =
                if (errorMessage == null) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun displayError(
        errorLayout: TextInputLayout,
        errorMessage: Int?
    ) {
        errorLayout.run {
            errorMessage?.let {
                setErrorTextColor(context?.getColorStateList(R.color.error_input_text))
                setErrorIconDrawable(R.drawable.ic_error)
                error = context?.getString(it)
            } ?: let {
                error = null
                errorIconDrawable = null
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
        super.closeKeyboard(view)
    }

    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_close, null)
        toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)
        toolbar.contentInsetStartWithNavigation = 0
    }

    override fun mapToItemDetailView(): ItemDetailViewModel {
        val hostname = inputHostname.text.toString()
        return ItemDetailViewModel(
            id = "",
            title = titleFromHostname(hostname),
            hostname = hostname,
            username = inputUsername.text.toString(),
            password = inputPassword.text.toString()
        )
    }
}
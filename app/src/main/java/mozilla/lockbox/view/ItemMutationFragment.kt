/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.focusChanges
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_create_item.view.*
import kotlinx.android.synthetic.main.fragment_edit_item.*
import kotlinx.android.synthetic.main.fragment_edit_item.view.*
import kotlinx.android.synthetic.main.fragment_edit_item.view.btnPasswordToggle
import kotlinx.android.synthetic.main.fragment_edit_item.view.inputHostname
import kotlinx.android.synthetic.main.fragment_edit_item.view.inputLayoutHostname
import kotlinx.android.synthetic.main.fragment_edit_item.view.inputLayoutPassword
import kotlinx.android.synthetic.main.fragment_edit_item.view.inputLayoutUsername
import kotlinx.android.synthetic.main.fragment_edit_item.view.inputPassword
import kotlinx.android.synthetic.main.fragment_edit_item.view.inputUsername
import kotlinx.android.synthetic.main.fragment_edit_item.view.saveEntryButton
import kotlinx.android.synthetic.main.fragment_edit_item.view.toolbar
import mozilla.lockbox.R
import mozilla.lockbox.presenter.ItemMutationView
import mozilla.lockbox.support.assertOnUiThread

open class ItemMutationFragment : BackableFragment(), ItemMutationView {
    override val togglePasswordClicks: Observable<Unit>
        get() = requireView().btnPasswordToggle.clicks().mergeWith(passwordFocus.map { Unit })

    override val closeEntryClicks: Observable<Unit>
        get() = requireView().toolbar.navigationClicks()

    override val saveEntryClicks: Observable<Unit>
        get() = requireView().saveEntryButton.clicks()

    override val hostnameChanged: Observable<String>
        get() = requireView().inputHostname.textChanges().map { it.toString() }

    override val usernameChanged: Observable<String>
        get() = requireView().inputUsername.textChanges().map { it.toString() }

    override val passwordChanged: Observable<String>
        get() = requireView().inputPassword.textChanges().map { it.toString() }

    override val hostnameFocus
        get() = focusChanges(requireView().inputHostname)

    override val passwordFocus
        get() = focusChanges(requireView().inputPassword)

    override val usernameFocus
        get() = focusChanges(requireView().inputUsername)

    override var isPasswordVisible: Boolean = false
        set(value) {
            assertOnUiThread()
            field = value
            updatePasswordVisibility(value)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view.toolbar)
        setupKeyboardFocus(view)
        setupItemDisplay(view)
    }

    open fun setupItemDisplay(view: View) {
        val layouts = arrayOf(
            view.inputLayoutHostname,
            view.inputLayoutUsername,
            view.inputLayoutPassword
        )

        for (layout in layouts) {
            layout?.hintTextColor = context?.getColorStateList(R.color.hint_edit_text)
            layout?.setHintTextAppearance(R.style.HintText)
            layout?.isHintAnimationEnabled = false
        }
        view.inputLayoutHostname.error = null
        view.inputHostname.error = null
        view.apply {
            toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)

            inputHostname.isFocusable = true
            inputHostname.isClickable = true

            inputUsername.isFocusable = true
            inputUsername.isClickable = true

            inputPassword.isFocusable = true
            inputPassword.isClickable = true
        }
    }

    private fun focusChanges(view: EditText): Observable<Boolean> =
        view.focusChanges().doOnNext { hasFocus ->
            if (!hasFocus) {
                closeKeyboard()
            } else {
                view.setSelection(view.length())
            }
        }

    open fun setupKeyboardFocus(view: View) {
        // Empty here, but will have implementations specific to create and edit.
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

    override fun displayHostnameError(errorMessage: Int?) {
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
}

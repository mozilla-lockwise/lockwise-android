/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import com.jakewharton.rxrelay2.ReplayRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_item_edit.*
import kotlinx.android.synthetic.main.fragment_item_edit.view.*
import kotlinx.android.synthetic.main.fragment_item_edit.view.toolbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.presenter.EditItemDetailView
import mozilla.lockbox.presenter.EditItemPresenter
import mozilla.lockbox.support.assertOnUiThread

@ExperimentalCoroutinesApi
class EditItemFragment : BackableFragment(), EditItemDetailView {

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
    }

    private val compositeDisposable = CompositeDisposable()

    override val deleteClicks: Observable<Unit>
        get() = view!!.deleteEntryButton.clicks()

    override val closeEntryClicks: ReplayRelay<Unit> = ReplayRelay.createWithSize<Unit>(1)

    override val saveEntryClicks: Observable<Unit>
        get() = view!!.saveEntryButton.clicks()

    // input validation
    override val hostnameChanged: Observable<CharSequence>
        get() = view!!.inputHostname.textChanges()
    // input validation
    override val usernameChanged: Observable<CharSequence>
        get() = view!!.inputUsername.textChanges()
    // input validation
    override val passwordChanged: Observable<CharSequence>
        get() = view!!.inputPassword.textChanges()

    override fun closeKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(view!!.windowToken, 0)
        }
    }

    private fun setupToolbar(toolbar: Toolbar) {
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_close, null)
        toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)
        toolbar.contentInsetStartWithNavigation = 0

        toolbar.navigationClicks()
            .subscribe {
                closeEntryClicks
            }
            .addTo(compositeDisposable)
    }

    private fun scrollToTop() {
//        cardView.layoutMode =
    }

    override fun updateItem(item: ItemDetailViewModel) {
        assertOnUiThread()
        toolbar.elevation = resources.getDimension(R.dimen.larger_toolbar_elevation)
        toolbar.title = item.title
        toolbar.entryTitle.text = item.title
        toolbar.entryTitle.gravity = Gravity.CENTER_VERTICAL

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
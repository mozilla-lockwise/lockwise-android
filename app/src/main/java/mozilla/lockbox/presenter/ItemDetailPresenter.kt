/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.model.titleFromHostname
import mozilla.lockbox.store.DataStore

interface ItemDetailView {
    var itemId: String?
    fun updateItem(item: ItemDetailViewModel)
    fun showToastNotification(@StringRes strId: Int)

    val usernameCopyClicks: Observable<Unit>
    val passwordCopyClicks: Observable<Unit>
    val togglePasswordClicks: Observable<Unit>

    var isPasswordVisible: Boolean
}

class ItemDetailPresenter(
    private val view: ItemDetailView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared

) : Presenter() {

    override fun onViewReady() {
        this.view.usernameCopyClicks
                .subscribe {
                    view.itemId?.let {
                        dataStore.get(it)
                                .subscribe {
                                    if (it!!.username!!.isNotEmpty()) {
                                        dispatcher.dispatch(ClipboardAction.CopyUsername(it.username!!))
                                        view.showToastNotification(R.string.toast_username_copied)
                                    }
                                }
                                .addTo(compositeDisposable)
                    }
                }
                .addTo(compositeDisposable)

        this.view.passwordCopyClicks
                .subscribe {
                    view.itemId?.let {
                        dataStore.get(it)
                                .subscribe {
                                    if (it!!.password.isNotEmpty()) {
                                        dispatcher.dispatch(ClipboardAction.CopyPassword(it.password))
                                        view.showToastNotification(R.string.toast_password_copied)
                                    }
                                }
                                .addTo(compositeDisposable)
                    }
                }
                .addTo(compositeDisposable)

        this.view.togglePasswordClicks
                .subscribe {
                    view.isPasswordVisible = view.isPasswordVisible.not()
                }
                .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        val itemId = view.itemId ?: return
        dataStore.get(itemId)
                .map {
                    ItemDetailViewModel(it.id, titleFromHostname(it.hostname), it.hostname, it.username, it.password)
                }
                .subscribe(view::updateItem)
                .addTo(compositeDisposable)

        view.isPasswordVisible = false
    }
}
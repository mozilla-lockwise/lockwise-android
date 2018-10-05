/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.model.titleFromHostname
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ClipboardStore

interface ItemDetailView {
    var itemId: String?
    fun updateItem(item: ItemDetailViewModel)
    fun copyNotification(strId: Int)

    val btnUsernameCopyClicks: Observable<Unit>
    val btnPasswordCopyClicks: Observable<Unit>
    val btnTogglePasswordClicks: Observable<Unit>

    var isPasswordVisible: Boolean
}

class ItemDetailPresenter(
    private val view: ItemDetailView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val clipboardStore: ClipboardStore = ClipboardStore.shared

) : Presenter() {

    override fun onViewReady() {

        this.view.btnUsernameCopyClicks
                .subscribe {
                    view.itemId?.let {
                        dataStore.get(it)
                                .subscribe {
                                    clipboardStore.clipboardCopy("username", it!!.username!!)
                                    view.copyNotification(R.string.toast_username_copied)
                                }
                                .addTo(compositeDisposable)
                    }
                }
                .addTo(compositeDisposable)

        this.view.btnPasswordCopyClicks
                .subscribe {
                    view.itemId?.let {
                        dataStore.get(it)
                                .subscribe {
                                    clipboardStore.clipboardCopy("password", it!!.password)
                                    view.copyNotification(R.string.toast_password_copied)
                                }
                                .addTo(compositeDisposable)
                    }
                }
                .addTo(compositeDisposable)

        this.view.btnTogglePasswordClicks
                .subscribe {
                    view.isPasswordVisible = view.isPasswordVisible.not()
                }
                .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        val itemId = view?.itemId ?: return
        dataStore.get(itemId)
                .map {
                    ItemDetailViewModel(it.id, titleFromHostname(it.hostname), it.hostname, it.username, it.password)
                }
                .subscribe(view::updateItem)
                .addTo(compositeDisposable)

        view.isPasswordVisible = false
    }
}
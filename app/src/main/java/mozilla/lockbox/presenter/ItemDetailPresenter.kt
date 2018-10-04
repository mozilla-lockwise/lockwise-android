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
import android.content.ClipData
import android.content.ClipboardManager

interface ItemDetailView {
    var itemId: String?
    fun updateItem(item: ItemDetailViewModel)
    fun copyNotification(strId: Int)
    fun updatePasswordField(visible: Boolean)

    val btnUsernameCopyClicks: Observable<Unit>
    val btnPasswordCopyClicks: Observable<Unit>
    val btnTogglePasswordClicks: Observable<Unit>
}

class ItemDetailPresenter(
    private val view: ItemDetailView,
    private val clipboardManager: ClipboardManager,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared

) : Presenter() {

    var isPasswordVisible: Boolean = false

    override fun onViewReady() {

        this.view.btnUsernameCopyClicks
                .subscribe {
                    view.itemId?.let {
                        dataStore.get(it)
                                .subscribe {
                                    clipboardCopy("username", it!!.username!!)
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
                                    clipboardCopy("password", it!!.password)
                                    view.copyNotification(R.string.toast_password_copied)
                                }
                                .addTo(compositeDisposable)
                    }
                }
                .addTo(compositeDisposable)

        this.view.btnTogglePasswordClicks
                .subscribe {
                    isPasswordVisible = isPasswordVisible.not()
                    view.updatePasswordField(isPasswordVisible)
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

        isPasswordVisible = false
    }

    private fun clipboardCopy(label: String, str: String) {

        val clip = ClipData.newPlainText(label, str)
        this.clipboardManager.primaryClip = clip
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore

interface ItemDetailView {
    fun updateItem(item: ItemDetailViewModel)
    fun showToastNotification(@StringRes strId: Int)

    val usernameCopyClicks: Observable<Unit>
    val passwordCopyClicks: Observable<Unit>
    val togglePasswordClicks: Observable<Unit>
    val hostnameClicks: Observable<Unit>

    var isPasswordVisible: Boolean
}

@ExperimentalCoroutinesApi
class ItemDetailPresenter(
    private val view: ItemDetailView,
    val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared
) : Presenter() {

    private var credentials: ServerPassword? = null

    override fun onViewReady() {
        handleClicks(view.usernameCopyClicks) {
            val username = it.username ?: ""
            if (username.isNotEmpty()) {
                dispatcher.dispatch(ClipboardAction.CopyUsername(username))
                dispatcher.dispatch(DataStoreAction.Touch(it.id))
                view.showToastNotification(R.string.toast_username_copied)
            }
        }

        handleClicks(view.passwordCopyClicks) {
            if (it.password.isNotEmpty()) {
                dispatcher.dispatch(ClipboardAction.CopyPassword(it.password))
                dispatcher.dispatch(DataStoreAction.Touch(it.id))
                view.showToastNotification(R.string.toast_password_copied)
            }
        }

        handleClicks(view.hostnameClicks) {
            if (it.hostname.isNotEmpty()) {
                dispatcher.dispatch(RouteAction.OpenWebsite(it.hostname))
            }
        }

        this.view.togglePasswordClicks
            .subscribe {
                view.isPasswordVisible = view.isPasswordVisible.not()
            }
            .addTo(compositeDisposable)

        view.isPasswordVisible = false

        // now set up the data.
        val itemId = this.itemId ?: return

        dataStore.get(itemId)
            .observeOn(mainThread())
            .doOnNext { credentials = it }
            .map { it.toDetailViewModel() }
            .subscribe(view::updateItem)
            .addTo(compositeDisposable)
    }

    private fun handleClicks(clicks: Observable<Unit>, withServerPassword: (ServerPassword) -> Unit) {
        clicks.subscribe {
                this.credentials?.let { password -> withServerPassword(password) }
            }
            .addTo(compositeDisposable)
    }
}
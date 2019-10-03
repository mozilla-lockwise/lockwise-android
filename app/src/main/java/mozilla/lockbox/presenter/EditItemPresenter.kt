/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.pushError

interface EditItemDetailView {
    var isPasswordVisible: Boolean
    val togglePasswordClicks: Observable<Unit>
    val togglePasswordVisibility: Observable<Unit>
    val deleteClicks: Observable<Unit>
    val learnMoreClicks: Observable<Unit>
    val closeEntryClicks: Observable<Unit>
    val saveEntryClicks: Observable<Unit>
    val hostnameChanged: Observable<CharSequence>
    val usernameChanged: Observable<CharSequence>
    val passwordChanged: Observable<CharSequence>
    fun updateItem(item: ItemDetailViewModel)
    fun closeKeyboard()
}

@ExperimentalCoroutinesApi
class EditItemPresenter(
    private val view: EditItemDetailView,
    val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : Presenter() {

    private var credentials: ServerPassword? = null

    override fun onViewReady() {
        val itemId = this.itemId ?: return

        dataStore.get(itemId)
            .observeOn(mainThread())
            .filterNotNull()
            .doOnNext { credentials = it }
            .map { it.toDetailViewModel() }
            .subscribe(view::updateItem)
            .addTo(compositeDisposable)

        view.isPasswordVisible = false

        itemDetailStore.isPasswordVisible
            .subscribe { view.isPasswordVisible = it }
            .addTo(compositeDisposable)

        view.togglePasswordVisibility
            .subscribe {
                dispatcher.dispatch(
                    ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not())
                )
            }
            .addTo(compositeDisposable)

        view.togglePasswordClicks
            .subscribe {
                dispatcher.dispatch(
                    ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not())
                )
            }
            .addTo(compositeDisposable)

        view.learnMoreClicks
            .subscribe {
                dispatcher.dispatch(RouteAction.OpenWebsite(Constant.Faq.uri))
            }
            .addTo(compositeDisposable)

        view.deleteClicks
            .subscribe {
                credentials?.let {
                    dispatcher.dispatch(DialogAction.DeleteConfirmationDialog(it))
                } ?: pushError(
                    NullPointerException("Credentials are null"),
                    "Error editing credential with id ${credentials?.id}"
                )
            }
            .addTo(compositeDisposable)

        view.closeEntryClicks
            .subscribe {
                dispatcher.dispatch(DialogAction.DiscardChangesDialog(credentials!!.id))
            }
            .addTo(compositeDisposable)

        view.hostnameChanged
            .subscribe {
                updateCredentials(newHostname = it)
            }
            .addTo(compositeDisposable)

        view.usernameChanged
            .subscribe {
                updateCredentials(newUsername = it)
            }
            .addTo(compositeDisposable)

        view.passwordChanged
            .subscribe {
                updateCredentials(newPassword = it)
            }
            .addTo(compositeDisposable)

        view.saveEntryClicks
            .subscribe {
                credentials?.let {
                    dispatcher.dispatch(DataStoreAction.UpdateItemDetail(it))
                    view.closeKeyboard()
                    dispatcher.dispatch(RouteAction.ItemList)
                } ?: pushError(
                    NullPointerException("Credentials are null"),
                    "Error editing credential with id ${credentials?.id}"
                )
            }
            .addTo(compositeDisposable)
    }

    private fun updateCredentials(
        newHostname: CharSequence? = null,
        newUsername: CharSequence? = null,
        newPassword: CharSequence? = null
    ) {
        credentials?.let { cred ->
            credentials = ServerPassword(
                id = cred.id,
                hostname = newHostname?.toString() ?: cred.hostname,
                username = newUsername?.toString() ?: cred.username,
                password = newPassword?.toString() ?: cred.password,
                httpRealm = cred.httpRealm,
                formSubmitURL = cred.formSubmitURL
            )
        } ?: pushError(
            NullPointerException("Credentials are null"),
            "Error editing credential with id ${credentials?.id}"
        )
    }
}
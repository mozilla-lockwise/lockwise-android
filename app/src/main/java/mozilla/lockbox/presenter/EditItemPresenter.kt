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
import mozilla.appservices.fxaclient.FxaException
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Constant
import java.lang.NullPointerException

interface EditItemDetailView {
    var isPasswordVisible: Boolean
    val togglePasswordClicks: Observable<Unit>
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

        view.togglePasswordClicks
            .subscribe {
                dispatcher.dispatch(ItemDetailAction.TogglePassword(view.isPasswordVisible.not()))
            }
            .addTo(compositeDisposable)

        view.learnMoreClicks
            .subscribe {
                dispatcher.dispatch(RouteAction.OpenWebsite(Constant.Faq.uri))
            }
            .addTo(compositeDisposable)

        view.deleteClicks
            .subscribe {
                dispatcher.dispatch(DataStoreAction.Delete(credentials))
                dispatcher.dispatch(RouteAction.ItemList)
            }
            .addTo(compositeDisposable)

        view.closeEntryClicks
            .subscribe {
                dispatcher.dispatch(DialogAction.DiscardChangesDialog(credentials!!.id))
            }
            .addTo(compositeDisposable)

        view.hostnameChanged
            .subscribe {
                updateCredentials(newHostname = it, newUsername = null, newPassword = null)
            }
            .addTo(compositeDisposable)

        view.usernameChanged
            .subscribe {
                updateCredentials(newHostname = null, newUsername = it, newPassword = null)
            }
            .addTo(compositeDisposable)

        view.passwordChanged
            .subscribe {
                updateCredentials(newHostname = null, newUsername = null, newPassword = it)
            }
            .addTo(compositeDisposable)

        view.saveEntryClicks
            .subscribe {
                dispatcher.dispatch(DataStoreAction.UpdateItemDetail(credentials))

                view.closeKeyboard()
//                dispatcher.dispatch(RouteAction.ItemDetail(credentials!!.id))
                dispatcher.dispatch(RouteAction.ItemList)
            }
            .addTo(compositeDisposable)
    }

    private fun updateCredentials(
        newHostname: CharSequence?,
        newUsername: CharSequence?,
        newPassword: CharSequence?
    ) {
        if(credentials != null) {
            credentials = ServerPassword(
                id = credentials!!.id,
                hostname = newHostname?.toString() ?: credentials!!.hostname,
                username = newUsername?.toString() ?: credentials!!.username,
                password = newPassword?.toString() ?: credentials!!.password,
                httpRealm = credentials!!.httpRealm
            )
        } else {
            pushError(NullPointerException("Credential is null."))
        }
    }


    private fun pushError(it: Throwable) {
        log.error("Error editing credential with id ${credentials?.id}")
        dispatcher.dispatch(SentryAction(it))
    }
}
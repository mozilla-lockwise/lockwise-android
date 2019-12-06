/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.text.TextUtils
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filter
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.pushError

interface EditItemView : ItemMutationView {
    override fun updateItem(item: ItemDetailViewModel)
}

@ExperimentalCoroutinesApi
class EditItemPresenter(
    private val view: EditItemView,
    val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : ItemMutationPresenter(view, itemId, dispatcher, dataStore, itemDetailStore) {

    override fun onViewReady() {
        super.onViewReady()

        val itemId = this.itemId ?: return

        val getItem = dataStore.get(itemId)
            .filterNotNull()
            .distinctUntilChanged()
            .doOnNext {
                credentialsAtStart = it
            }

        getItem
            .filter { credentialsToSave == null }
            .map { it.toDetailViewModel() }
            .observeOn(mainThread())
            .subscribe(view::updateItem)
            .addTo(compositeDisposable)

        Observables.combineLatest(getItem, dataStore.list)
            .map { (item, list) ->
                list.filter(
                    hostname = item.hostname,
                    httpRealm = item.httpRealm,
                    formSubmitURL = item.formSubmitURL
                )
                    .map { it.username }
                    .toSet()
                    .minus(item.username)
            }
            .subscribe { unavailableUsernames = it }
            .addTo(compositeDisposable)

        itemDetailStore.isEditing
            .distinctUntilChanged()
            .filter { !it }
            .flatMapIterable {
                view.closeKeyboard()
                listOf(RouteAction.ItemList, RouteAction.ItemDetail(itemId))
            }
            .subscribe(dispatcher::dispatch)
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
                checkSaveChanges(credentialsToSave)
            }
            .addTo(compositeDisposable)
    }

    override fun observeChangeErrors() {
        Observables.combineLatest(
            view.usernameChanged.map(this::usernameError),
            view.passwordChanged.map(this::passwordError)
        )
            .subscribe { (usernameError, passwordError) ->
                view.displayUsernameError(usernameError.value)
                view.displayPasswordError(passwordError.value)

                view.setSaveEnabled(usernameError.value == null && passwordError.value == null)
            }
            .addTo(compositeDisposable)
    }

    override fun checkSaveChanges(credentialsToSave: ServerPassword?) {
        // When there's something changed, then save them,
        // otherwise, go back to the item detail screen.
        if (credentialsAtStart != null && credentialsToSave != null)
            ItemDetailAction.SaveChanges(credentialsAtStart!!, credentialsToSave!!)
        else
            ItemDetailAction.EndEditing(itemId ?: return)
    }

    override fun checkDismissChanges(itemId: String?) =
    // When something has changes, then check with a dialog.
        // otherwise, go back to the item detail screen.
        credentialsToSave?.let { DialogAction.DiscardChangesDialog(itemId ?: "") }
            ?: ItemDetailAction.EndEditing(itemId ?: "")

    override fun onBackPressed(): Boolean {
        val itemId = credentialsAtStart?.id ?: return false
        dispatcher.dispatch(checkDismissChanges(itemId))
        return true
    }

    override fun usernameError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> null
            unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
            else -> null
        }.asOptional()

    override fun passwordError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.password_invalid_text
            else -> null
        }.asOptional()

    private fun updateCredentials(
        newHostname: String? = null,
        newUsername: String? = null,
        newPassword: String? = null
    ) {
        val old: ServerPassword = credentialsEdited ?: credentialsAtStart ?: return pushError(
            NullPointerException("Credentials are null"),
            "Error editing credential with id $itemId"
        )
        credentialsEdited = old.copy(
            hostname = newHostname ?: old.hostname,
            username = newUsername ?: old.username,
            password = newPassword ?: old.password
        )
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.text.TextUtils
import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.filter
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.pushError

interface EditItemDetailView {
    var isPasswordVisible: Boolean
    val togglePasswordClicks: Observable<Unit>
    val togglePasswordVisibility: Observable<Unit>
    val closeEntryClicks: Observable<Unit>
    val saveEntryClicks: Observable<Unit>
    val hostnameChanged: Observable<String>
    val usernameChanged: Observable<String>
    val passwordChanged: Observable<String>
    fun updateItem(item: ItemDetailViewModel)
    fun closeKeyboard()
    fun displayUsernameError(@StringRes errorMessage: Int? = null)
    fun displayPasswordError(@StringRes errorMessage: Int? = null)
    fun setSaveEnabled(enabled: Boolean)
}

@ExperimentalCoroutinesApi
class EditItemPresenter(
    private val view: EditItemDetailView,
    val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : Presenter() {

    // These should move to the ItemDetailStore.
    // https://github.com/mozilla-lockwise/lockwise-android/issues/977
    private var credentials: ServerPassword? = null
    private var edited: ServerPassword? = null
    private var unavailableUsernames: Set<String?> = emptySet()

    override fun onViewReady() {
        val itemId = this.itemId ?: return

        view.isPasswordVisible = false

        val getItem = dataStore.get(itemId)
            .filterNotNull()
            .doOnNext {
                credentials = it
                edited = it
            }

        getItem
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

        itemDetailStore.isPasswordVisible
            .subscribe { view.isPasswordVisible = it }
            .addTo(compositeDisposable)

        itemDetailStore.isEditing
            .distinctUntilChanged()
            .filter { !it }
            .flatMapIterable {
                edited = null
                view.closeKeyboard()
                listOf(RouteAction.ItemList, RouteAction.ItemDetail(itemId))
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.togglePasswordVisibility
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.togglePasswordClicks
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.closeEntryClicks
            .map { checkDismissChanges(itemId) }
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

        view.saveEntryClicks
            .map {
                edited?.let { ItemDetailAction.SaveChanges(it) }
                    ?: ItemDetailAction.DiscardChanges(itemId)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun checkDismissChanges(itemId: String) =
        edited?.let {
            if (it != credentials) {
                DialogAction.DiscardChangesDialog(itemId)
            } else {
                null
            }
        } ?: ItemDetailAction.DiscardChanges(itemId)

    override fun onBackPressed(): Boolean {
        val itemId = credentials?.id ?: return false
        dispatcher.dispatch(checkDismissChanges(itemId))
        return true
    }

    private fun usernameError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> null
            unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
            else -> null
        }.asOptional()

    private fun passwordError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.password_invalid_text
            else -> null
        }.asOptional()

    private fun updateCredentials(
        newHostname: String? = null,
        newUsername: String? = null,
        newPassword: String? = null
    ) {
        val old = edited ?: credentials ?: return pushError(
            NullPointerException("Credentials are null"),
            "Error editing credential with id $itemId"
        )
        edited = old.copy(
            hostname = newHostname ?: old.hostname,
            username = newUsername ?: old.username,
            password = newPassword ?: old.password
        )
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filter
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.pushError

interface ItemMutationView {
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
abstract class ItemMutationPresenter(
    private val view: ItemMutationView,
    private val itemId: String?,
    private val dispatcher: Dispatcher,
    private val dataStore: DataStore = DataStore.shared,
    private val itemDetailStore: ItemDetailStore
) : Presenter() {

    var unavailableUsernames: Set<String?> = emptySet()

    abstract fun checkDismissChanges(itemId: String?): Action
    abstract fun checkSaveChanges(credentialsToSave: ServerPassword?)
    abstract override fun onBackPressed(): Boolean
    abstract fun observeChangeErrors()
    abstract fun usernameError(inputText: String): Optional<Int>
    abstract fun passwordError(inputText: String): Optional<Int>

    // These should move to the ItemDetailStore.
    // https://github.com/mozilla-lockwise/lockwise-android/issues/977
    var credentialsAtStart: ServerPassword? = null
        set(newValue) {
            credentialsEdited = null
            field = newValue
        }
    var credentialsEdited: ServerPassword? = null

    val credentialsToSave: ServerPassword?
        get() {
            return credentialsEdited?.let {
                if (it != credentialsAtStart) {
                    it
                } else {
                    null
                }
            }
        }

    override fun onViewReady() {
        val itemId = this.itemId ?: return
        view.isPasswordVisible = false

        retrieveUnavailableUsernames(itemId)

        itemDetailStore.isEditing
            .distinctUntilChanged()
            .filter { !it }
            .flatMapIterable {
                view.closeKeyboard()
                listOf(RouteAction.ItemList, RouteAction.ItemDetail(itemId))
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        checkPasswordVisibility()

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

        observeChangeErrors()

        view.saveEntryClicks
            .subscribe {
                checkSaveChanges(credentialsToSave)
            }
            .addTo(compositeDisposable)
    }

    private fun retrieveUnavailableUsernames(itemId: String) {
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
    }

    private fun checkPasswordVisibility() {
        itemDetailStore.isPasswordVisible
            .subscribe { view.isPasswordVisible = it }
            .addTo(compositeDisposable)

        view.togglePasswordVisibility
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.togglePasswordClicks
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun updateCredentials(
        newHostname: String? = null,
        newUsername: String? = null,
        newPassword: String? = null
    ) {
        val old = credentialsEdited ?: credentialsAtStart ?: return pushError(
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

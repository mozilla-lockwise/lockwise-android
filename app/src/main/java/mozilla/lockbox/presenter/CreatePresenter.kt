/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.text.TextUtils
import android.webkit.URLUtil
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
import mozilla.lockbox.extensions.filter
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.pushError

interface CreateView {
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
class CreatePresenter(
    private val view: CreateView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : Presenter() {

    // These should move to the ItemDetailStore.
    // https://github.com/mozilla-lockwise/lockwise-android/issues/977
    private var credentialsAtStart: ServerPassword? = null
        set(newValue) {
            credentialsEdited = null
            field = newValue
        }
    private var credentialsEdited: ServerPassword? = null

    private val credentialsToSave: ServerPassword?
        get() {
            return credentialsEdited?.let {
                if (it != credentialsAtStart) {
                    it
                } else {
                    null
                }
            }
        }

    private var unavailableUsernames: Set<String?> = emptySet()

    override fun onViewReady() {
        view.isPasswordVisible = false

        // when do we want to check for dupes?
        dataStore.list
            .map { list ->
                list.filter(
                        hostname = credentialsToSave?.hostname,
                        httpRealm = credentialsToSave?.httpRealm,
                        formSubmitURL = credentialsToSave?.formSubmitURL
                    )
                    .map { it.username }
                    .toSet()
                    .minus(credentialsToSave.username)
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
                view.closeKeyboard()
                listOf(RouteAction.ItemList)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.closeEntryClicks
            .map { checkDismissChanges() }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        Observables.combineLatest(
            view.hostnameChanged.map(this::hostnameError),
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
                // When there's something changed, then save them,
                // otherwise, go back to the item list.
                credentialsToSave?.let { ItemDetailAction.Create(it) }
                    ?: ItemDetailAction.DiscardManualCreate
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    private fun checkDismissChanges() =
        // When something has changes, then check with a dialog.
        // otherwise, go back to the item detail screen.
        credentialsToSave?.let { DialogAction.DiscardChangesCreateDialog }
            ?: ItemDetailAction.DiscardManualCreate

    override fun onBackPressed(): Boolean {
        dispatcher.dispatch(checkDismissChanges())
        return true
    }

    private fun hostnameError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.hostname_empty_invalid_text
            !URLUtil.isHttpUrl(inputText) || !URLUtil.isHttpsUrl(inputText) -> R.string.hostname_invalid_text
            else -> null
        }.asOptional()

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
}

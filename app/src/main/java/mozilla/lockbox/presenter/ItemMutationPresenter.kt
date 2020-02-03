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
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.ItemDetailStore

interface ItemMutationView {
    var isPasswordVisible: Boolean
    val togglePasswordClicks: Observable<Unit>
    val closeEntryClicks: Observable<Unit>
    val saveEntryClicks: Observable<Unit>

    val hostnameChanged: Observable<String>
    val usernameChanged: Observable<String>
    val passwordChanged: Observable<String>

    val passwordFocus: Observable<Boolean>
    val usernameFocus: Observable<Boolean>
    val hostnameFocus: Observable<Boolean>

    fun closeKeyboard()
    fun displayHostnameError(@StringRes errorMessage: Int? = null)
    fun displayUsernameError(@StringRes errorMessage: Int? = null)
    fun displayPasswordError(@StringRes errorMessage: Int? = null)
    fun setSaveEnabled(enabled: Boolean)
}

@ExperimentalCoroutinesApi
abstract class ItemMutationPresenter(
    private val view: ItemMutationView,
    private val dispatcher: Dispatcher,
    private val itemDetailStore: ItemDetailStore
) : Presenter() {

    override fun onViewReady() {
        view.isPasswordVisible = false

        itemDetailStore.isPasswordVisible
            .subscribe { view.isPasswordVisible = it }
            .addTo(compositeDisposable)

        view.togglePasswordClicks
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.closeEntryClicks
            .flatMap { itemDetailStore.isDirty.take(1) }
            .map { dismissChangesAction(it) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.saveEntryClicks
            .flatMap { itemDetailStore.isDirty.take(1) }
            .flatMapIterable { saveChangesActions(it) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.usernameChanged
            .map {
                ItemDetailAction.EditField(username = it)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.passwordChanged
            .map {
                ItemDetailAction.EditField(password = it)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        val fieldsChanged = Observables.combineLatest(
            view.hostnameChanged, view.usernameChanged, view.passwordChanged
        )

        val fieldsShowingErrors = Observables.combineLatest(
            focusLostOnce(view.hostnameFocus),
            focusLostOnce(view.usernameFocus),
            focusLostOnce(view.passwordFocus)
        )

        Observables.combineLatest(fieldsChanged, itemDetailStore.unavailableUsernames, fieldsShowingErrors)
            .map { (fields, unavailableUsernames, showingErrors) ->
                Triple(
                    hostnameError(fields.first, showingErrors.first),
                    usernameError(fields.second, unavailableUsernames, showingErrors.second),
                    passwordError(fields.third, showingErrors.third)
                )
            }
            .observeOn(mainThread())
            .subscribe { (hostnameError, usernameError, passwordError) ->
                val errorFunctions = listOf(
                    view::displayHostnameError to hostnameError,
                    view::displayUsernameError to usernameError,
                    view::displayPasswordError to passwordError
                )

                errorFunctions.forEach { (displayFunction, error) ->
                    val errorMessage = if (error != R.string.hidden_credential_mutation_error) {
                        // We use a marker error for an error we don't tell the user about,
                        // but will cause the save button to be disabled.
                        error
                    } else {
                        // `null` clears an existing error.
                        null
                    }
                    displayFunction.invoke(errorMessage)
                }

                val errorDetected = errorFunctions.fold(false) { acc, (_, error) ->
                    acc || (error != null)
                }

                view.setSaveEnabled(!errorDetected)
            }
            .addTo(compositeDisposable)

        // Configure what happens when we finish with this screen.
        itemDetailStore.isEditing
            .distinctUntilChanged()
            .filter { !it }
            .flatMapIterable {
                view.closeKeyboard()
                endEditingActions()
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    // Returns an observable that emits with the same frequency as `focusChanges`
    // but emits `true` if and only if the input observable has emitted `true` first time and then a
    // subsequent `false`.
    private fun focusLostOnce(focusChange: Observable<Boolean>): Observable<Boolean> {
        var focusOnce = false
        var lostFocusOnce = false

        return focusChange
            .doOnNext {
                if (it) {
                    focusOnce = true
                } else if (focusOnce) {
                    lostFocusOnce = true
                }
            }
            .map {
                lostFocusOnce
            }
    }

    open fun usernameError(
        inputText: String,
        unavailableUsernames: Set<String>,
        showingErrors: Boolean
    ) =
        when {
            TextUtils.isEmpty(inputText) -> null
            unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
            else -> null
        }

    open fun passwordError(inputText: String, showingErrors: Boolean) =
        when {
            TextUtils.isEmpty(inputText) -> if (showingErrors) {
                R.string.password_invalid_text
            } else {
                R.string.hidden_credential_mutation_error
            }
            else -> null
        }

    abstract fun hostnameError(inputText: String, showingErrors: Boolean): Int?

    override fun onBackPressed(): Boolean {
        itemDetailStore.isDirty
            .take(1)
            .map {
                dismissChangesAction(it)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
        return true
    }

    abstract fun dismissChangesAction(hasChanges: Boolean): Action
    abstract fun saveChangesActions(hasChanges: Boolean): List<Action>
    abstract fun endEditingActions(): List<Action>
}

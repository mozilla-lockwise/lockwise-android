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
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional

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
            .map { saveChangesAction(it) }
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

        val blurredOnce = Observables.combineLatest(
            blurredOnce(view.hostnameFocus),
            blurredOnce(view.usernameFocus),
            blurredOnce(view.passwordFocus)
        )

        Observables.combineLatest(fieldsChanged, itemDetailStore.unavailableUsernames, blurredOnce)
            .map { (fields, unavailableUsernames, blurredOnce3) ->
                Triple(
                    hostnameError(fields.first, blurredOnce3.first),
                    usernameError(fields.second, unavailableUsernames, blurredOnce3.second),
                    passwordError(fields.third, blurredOnce3.third)
                )
            }
            .observeOn(mainThread())
            .subscribe { (hostnameError, usernameError, passwordError) ->
                val errorFunctions = listOf(
                    view::displayHostnameError to hostnameError.value,
                    view::displayUsernameError to usernameError.value,
                    view::displayPasswordError to passwordError.value
                )

                errorFunctions.forEach { (fn, error) ->
                    val toDisplay = if (error != R.string.undisplayed_credential_mutation_error) {
                        // We use a marker error for an error we don't tell the user about,
                        // but will cause the save button to be disabled.
                        error
                    } else {
                        // `null` clears an existing error.
                        null
                    }
                    fn.invoke(toDisplay)
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
                endEditingAction()
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    fun blurredOnce(focusChange: Observable<Boolean>): Observable<Boolean> {
        var focusOnce = false
        var blurredOnce = false

        return focusChange
            .doOnNext {
                if (it) {
                    focusOnce = true
                } else if (focusOnce) {
                    blurredOnce = true
                }
            }
            .map {
                blurredOnce
            }
    }

    open fun usernameError(
        inputText: String,
        unavailableUsernames: Set<String>,
        blurredOnce: Boolean
    ) =
        when {
            TextUtils.isEmpty(inputText) -> null
            unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
            else -> null
        }.asOptional()

    open fun passwordError(inputText: String, blurredOnce: Boolean) =
        when {
            TextUtils.isEmpty(inputText) -> if (blurredOnce) {
                R.string.password_invalid_text
            } else {
                R.string.undisplayed_credential_mutation_error
            }
            else -> null
        }.asOptional()

    abstract fun hostnameError(inputText: String, blurredOnce: Boolean): Optional<Int>

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
    abstract fun saveChangesAction(hasChanges: Boolean): ItemDetailAction
    abstract fun endEditingAction(): List<Action>
}

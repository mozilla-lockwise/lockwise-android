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

        Observables.combineLatest(fieldsChanged, itemDetailStore.unavailableUsernames)
        .map { (fields, unavailableUsernames) ->
            Triple(
                hostnameError(fields.first),
                usernameError(fields.second, unavailableUsernames),
                passwordError(fields.third)
            )
        }
        .observeOn(mainThread())
        .subscribe { (hostnameError, usernameError, passwordError) ->
            view.displayHostnameError(hostnameError.value)
            view.displayUsernameError(usernameError.value)
            view.displayPasswordError(passwordError.value)

            view.setSaveEnabled(hostnameError.value == null && usernameError.value == null && passwordError.value == null)
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

    open fun usernameError(
        inputText: String,
        unavailableUsernames: Set<String>
    ) =
        when {
            TextUtils.isEmpty(inputText) -> null
            unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
            else -> null
        }.asOptional()

    open fun passwordError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.password_invalid_text
            else -> null
        }.asOptional()

    abstract fun hostnameError(inputText: String): Optional<Int>

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

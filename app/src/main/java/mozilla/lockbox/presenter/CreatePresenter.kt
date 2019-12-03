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
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.asOptional

interface CreateView {
    var isPasswordVisible: Boolean
    val togglePasswordClicks: Observable<Unit>
    val togglePasswordVisibility: Observable<Unit>
    val closeEntryClicks: Observable<Unit>
    val saveEntryClicks: Observable<Unit>
    val hostnameChanged: Observable<String>
    val usernameChanged: Observable<String>
    val passwordChanged: Observable<String>
    fun closeKeyboard()
    fun mapToItemDetailView(): ItemDetailViewModel
    fun displayHostnameError(@StringRes errorMessage: Int? = null)
    fun displayUsernameError(@StringRes errorMessage: Int? = null)
    fun displayPasswordError(@StringRes errorMessage: Int? = null)
    fun setSaveEnabled(enabled: Boolean)
}

@ExperimentalCoroutinesApi
class CreatePresenter(
    private val view: CreateView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : Presenter() {
    private var _saveEnabled = false

    override fun onViewReady() {
        view.isPasswordVisible = false

        view.togglePasswordVisibility
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.togglePasswordClicks
            .map { ItemDetailAction.SetPasswordVisibility(view.isPasswordVisible.not()) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        itemDetailStore.isPasswordVisible
            .subscribe { view.isPasswordVisible = it }
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
            .subscribe {
//                    (hostnameError, usernameError, passwordError) ->
//                view.displayUsernameError(usernameError.value)
//                view.displayPasswordError(passwordError.value)

                _saveEnabled = true
//              _saveEnabled = hostnameError.value == null && usernameError.value == null && passwordError.value == null
                view.setSaveEnabled(_saveEnabled)
            }
            .addTo(compositeDisposable)

        view.saveEntryClicks
            .subscribe {
                dispatcher.dispatch(ItemDetailAction.Create(view.mapToItemDetailView()))
//                    ?: ItemDetailAction.DiscardManualCreate
                dispatcher.dispatch(RouteAction.ItemList)
            }
            .addTo(compositeDisposable)
    }

    // DialogAction.DiscardChangesCreateDialog to be used for https://github.com/mozilla-lockwise/lockwise-android/issues/822
    private fun checkDismissChanges() = RouteAction.DiscardCreateNoChanges

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

    // TODO in https://github.com/mozilla-lockwise/lockwise-android/issues/822
    // unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
    private fun usernameError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> null
            else -> null
        }.asOptional()

    private fun passwordError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.password_invalid_text
            else -> null
        }.asOptional()
}
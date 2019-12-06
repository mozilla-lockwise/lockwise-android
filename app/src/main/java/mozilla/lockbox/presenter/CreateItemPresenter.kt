/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.text.TextUtils
import android.webkit.URLUtil
import androidx.annotation.StringRes
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional

interface CreateItemView : ItemMutationView {
    fun mapToItemDetailView(): ItemDetailViewModel
    fun displayHostnameError(@StringRes errorMessage: Int? = null)
}

@ExperimentalCoroutinesApi
class CreateItemPresenter(
    private val view: CreateItemView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    dataStore: DataStore = DataStore.shared,
    itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : ItemMutationPresenter(view, "", dispatcher, dataStore, itemDetailStore) {

    private var _saveEnabled = false

    override fun onViewReady() {
        super.onViewReady()
        view.isPasswordVisible = false

        view.saveEntryClicks
            .subscribe {
                checkSaveChanges(credentialsToSave)
            }
            .addTo(compositeDisposable)
    }

    override fun checkSaveChanges(credentialsToSave: ServerPassword?) {
        dispatcher.dispatch(ItemDetailAction.Create(view.mapToItemDetailView()))
        // ?: ItemDetailAction.DiscardManualCreate
        dispatcher.dispatch(RouteAction.ItemList)
    }

    // TODO in https://github.com/mozilla-lockwise/lockwise-android/issues/822
    override fun observeChangeErrors() {
        Observables.combineLatest(
            view.hostnameChanged.map(this::hostnameError),
            view.usernameChanged.map(this::usernameError),
            view.passwordChanged.map(this::passwordError)
        )
            .subscribe {
                // (hostnameError, usernameError, passwordError) ->
                //      view.displayUsernameError(usernameError.value)
                //      view.displayPasswordError(passwordError.value)

                _saveEnabled = true
                // _saveEnabled = hostnameError.value == null && usernameError.value == null && passwordError.value == null
                view.setSaveEnabled(_saveEnabled)
            }
            .addTo(compositeDisposable)
    }

    // TODO in https://github.com/mozilla-lockwise/lockwise-android/issues/822
    // DialogAction.DiscardChangesCreateDialog
    override fun checkDismissChanges(itemId: String?): Action = RouteAction.DiscardCreateNoChanges

    override fun onBackPressed(): Boolean {
        dispatcher.dispatch(checkDismissChanges(null))
        return true
    }

    // TODO in https://github.com/mozilla-lockwise/lockwise-android/issues/822
    private fun hostnameError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.hostname_empty_invalid_text
            !URLUtil.isHttpUrl(inputText) || !URLUtil.isHttpsUrl(inputText) -> R.string.hostname_invalid_text
            else -> null
        }.asOptional()

    // TODO in https://github.com/mozilla-lockwise/lockwise-android/issues/822
    // get list of unavailable usernames
    // unavailableUsernames.contains(inputText) -> R.string.username_duplicate_exists
    override fun usernameError(inputText: String): Optional<Int> =
        when {
            TextUtils.isEmpty(inputText) -> null
            else -> null
        }.asOptional()

    // TODO in https://github.com/mozilla-lockwise/lockwise-android/issues/822
    override fun passwordError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.password_invalid_text
            else -> null
        }.asOptional()
}
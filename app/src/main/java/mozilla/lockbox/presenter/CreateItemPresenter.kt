/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.text.TextUtils
import android.webkit.URLUtil
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.ToastNotificationAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.ItemDetailStore

interface CreateItemView : ItemMutationView

private val minimalHostRegex = (
        "^https?" + // scheme
        "://" + // ://
        "(\\w+\\.\\w+)[^\\s]*$" // minimal host
    ).toRegex()

@ExperimentalCoroutinesApi
class CreateItemPresenter(
    private val view: CreateItemView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : ItemMutationPresenter(view, dispatcher, itemDetailStore) {

    override fun onViewReady() {
        dispatcher.dispatch(ItemDetailAction.BeginCreateItemSession)
        view.isPasswordVisible = false

        super.onViewReady()

        view.hostnameChanged
            .map {
                ItemDetailAction.EditField(hostname = it)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    override fun saveChangesActions(hasChanges: Boolean): List<Action> {
        return if (hasChanges) {
            listOf(
                ItemDetailAction.CreateItemSaveChanges,
                ToastNotificationAction.ShowSuccessfulCreateToast,
                ItemDetailAction.EndCreateItemSession
            )
        } else {
            listOf(ItemDetailAction.EndCreateItemSession)
        }
    }

    override fun dismissChangesAction(hasChanges: Boolean): List<Action> =
        if (hasChanges) {
            listOf(DialogAction.DiscardChangesCreateDialog)
        } else {
            listOf(ItemDetailAction.EndCreateItemSession, RouteAction.ItemList)
        }

    override fun endEditingActions(): List<Action> {
        val list = itemDetailStore.findSavedItem().blockingIterable().iterator().next()
        return listOf(
            RouteAction.ItemList,
            RouteAction.DisplayItem(list.first().id)
        )
    }

    override fun hostnameError(inputText: String, showingErrors: Boolean): Int? =
        when {
            TextUtils.isEmpty(inputText) ->
                R.string.hostname_empty_invalid_text `when` showingErrors

            inputText.length <= 7 && "http://".startsWith(inputText) ->
                R.string.hostname_invalid_text `when` showingErrors

            inputText.length <= 8 && "https://".startsWith(inputText) ->
                R.string.hostname_invalid_text `when` showingErrors

            !URLUtil.isHttpUrl(inputText) && !URLUtil.isHttpsUrl(inputText) ->
                R.string.hostname_invalid_text

            !minimalHostRegex.matches(inputText) ->
                R.string.hostname_invalid_host `when` showingErrors

            else -> null
        }

    private infix fun Int.`when`(showingErrors: Boolean) =
        if (showingErrors) {
            this
        } else {
            R.string.hidden_credential_mutation_error
        }
}

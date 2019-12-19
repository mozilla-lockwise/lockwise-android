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
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.asOptional

interface CreateItemView : ItemMutationView

@ExperimentalCoroutinesApi
class CreateItemPresenter(
    private val view: CreateItemView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    itemDetailStore: ItemDetailStore = ItemDetailStore.shared
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

    override fun saveChangesAction(hasChanges: Boolean): ItemDetailAction {
        return if (hasChanges) {
            ItemDetailAction.CreateItemSaveChanges
        } else {
            ItemDetailAction.EndCreateSession
        }
    }

    override fun dismissChangesAction(hasChanges: Boolean): Action {
        return if (hasChanges) {
            DialogAction.DiscardChangesCreateDialog
        } else {
            ItemDetailAction.EndCreateSession
        }
    }

    override fun endEditingAction(): List<Action> {
        return listOf(RouteAction.ItemList)
    }

    override fun hostnameError(inputText: String) =
        when {
            TextUtils.isEmpty(inputText) -> R.string.hostname_empty_invalid_text
            !URLUtil.isHttpUrl(inputText) && !URLUtil.isHttpsUrl(inputText) -> R.string.hostname_invalid_text
            else -> null
        }.asOptional()
}

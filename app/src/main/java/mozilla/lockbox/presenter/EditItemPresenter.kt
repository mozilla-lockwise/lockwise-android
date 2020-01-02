/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional

interface EditItemView : ItemMutationView {
    fun updateItem(item: ItemDetailViewModel)
}

@ExperimentalCoroutinesApi
class EditItemPresenter(
    private val view: EditItemView,
    private val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val itemDetailStore: ItemDetailStore = ItemDetailStore.shared
) : ItemMutationPresenter(view, dispatcher, itemDetailStore) {

    override fun onViewReady() {
        val itemId = this.itemId ?: return
        dispatcher.dispatch(ItemDetailAction.BeginEditItemSession(itemId))

        itemDetailStore.originalItem
            .filterNotNull()
            // so we don't overwrite changes when we come back from an
            // interrupt.
            .flatMap { item ->
                itemDetailStore.isDirty
                    .take(1)
                    .map { item to it }
            }
            .filter { (_, dirty) -> !dirty }
            .map { (item, _) -> item.toDetailViewModel() }
            .observeOn(mainThread())
            .subscribe(view::updateItem)
            .addTo(compositeDisposable)

        super.onViewReady()
    }

    override fun saveChangesAction(hasChanges: Boolean) =
        itemId?.let {
            if (hasChanges) {
                ItemDetailAction.EditItemSaveChanges
            } else {
                null
            }
        } ?: ItemDetailAction.EndEditItemSession

    override fun dismissChangesAction(hasChanges: Boolean) =
        itemId?.let {
            if (hasChanges) {
                DialogAction.DiscardChangesDialog(it)
            } else {
                null
            }
        } ?: ItemDetailAction.EndEditItemSession

    override fun endEditingAction(): List<Action> {
        return itemId?.let {
            listOf(RouteAction.ItemList, RouteAction.DisplayItem(it))
        } ?: listOf(RouteAction.ItemList)
    }

    override fun hostnameError(inputText: String): Optional<Int> = null.asOptional()
}

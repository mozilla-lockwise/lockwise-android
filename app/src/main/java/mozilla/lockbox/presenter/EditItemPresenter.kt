/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.toDetailViewModel
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore

interface EditItemDetailView {
    val deleteClicks: Observable<Unit>
    val closeEntryClicks: Observable<Unit>
    val saveEntryClicks: Observable<Unit>
    val hostnameChanged: Observable<CharSequence>
    val usernameChanged: Observable<CharSequence>
    val passwordChanged: Observable<CharSequence>
    fun updateItem(item: ItemDetailViewModel)
    fun closeKeyboard()
}

@ExperimentalCoroutinesApi
class EditItemPresenter(
    private val view: EditItemDetailView,
    val itemId: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared
) : Presenter() {

    private var credentials: ServerPassword? = null

    override fun onViewReady() {
        val itemId = this.itemId ?: return

        dataStore.get(itemId)
            .observeOn(mainThread())
            .filterNotNull()
            .doOnNext { credentials = it }
            .map { it.toDetailViewModel() }
            .subscribe(view::updateItem)
            .addTo(compositeDisposable)

        view.deleteClicks
            .subscribe {
                dispatcher.dispatch(DataStoreAction.Delete(credentials))
                dispatcher.dispatch(RouteAction.ItemList)
            }
            .addTo(compositeDisposable)

        view.closeEntryClicks
            .subscribe {
                dispatcher.dispatch(DialogAction.DiscardChangesDialog(credentials!!.id))
            }
            .addTo(compositeDisposable)

        view.saveEntryClicks
            .subscribe {
                dispatcher.dispatch(DataStoreAction.UpdateItemDetail(credentials!!))
                view.closeKeyboard()
                dispatcher.dispatch(RouteAction.ItemDetail(credentials!!.id))
            }
            .addTo(compositeDisposable)
    }
}
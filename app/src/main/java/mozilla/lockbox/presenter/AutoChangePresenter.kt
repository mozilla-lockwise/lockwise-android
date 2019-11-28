/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import android.webkit.WebView
import androidx.annotation.StringRes
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.autochange.AutoChangeHandler
import mozilla.lockbox.autochange.WebViewAutoChangeHandler
import mozilla.lockbox.autochange.noopPasswordGenerator
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.DataStore

interface AutoChangeView {
    val webView: WebView

    fun hideProgressToast()
    fun showProgressText(@StringRes message: Int)
}

@ExperimentalCoroutinesApi
class AutoChangePresenter(
    val context: Context,
    val view: AutoChangeView,
    val itemId: String,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared
) : Presenter() {

    override fun onViewReady() {
        dataStore.get(itemId)
            .filterNotNull()
            .take(1)
            .map { item ->
                WebViewAutoChangeHandler(
                    context,
                    webView = view.webView,
                    originalItem = item,
                    passwordGenerator = noopPasswordGenerator
                ) to item
            }
            .subscribe { (handler, item) ->
                setupAutoChangeHandler(handler, item)
            }
            .addTo(compositeDisposable)
    }

    fun setupAutoChangeHandler(
        autoChangeHandler: AutoChangeHandler,
        item: ServerPassword
    ) {
        autoChangeHandler.progress
            .subscribe {
                view.showProgressText(it.message)
            }
            .addTo(compositeDisposable)

        autoChangeHandler.invoke()
            .doOnComplete {
                dispatcher.dispatch(RouteAction.ItemList)
                dispatcher.dispatch(RouteAction.DisplayItem(item.id))
            }
            .map { DataStoreAction.UpdateItemDetail(item, it) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }
}

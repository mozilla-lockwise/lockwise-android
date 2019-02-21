/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.NetworkStore

interface WebPageView {
    var webViewObserver: Consumer<String>?
//    val retryNetworkConnectionClicks: Observable<Unit>
    fun handleNetworkError(networkErrorVisibility: Boolean)
    fun loadURL(url: String)
}

class AppWebPagePresenter(
    val view: WebPageView,
    val url: String?,
    private val networkStore: NetworkStore = NetworkStore.shared,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        networkStore.isConnected
            .subscribe(view::handleNetworkError)
            .addTo(compositeDisposable)

//        view.retryNetworkConnectionClicks.subscribe {
//            dispatcher.dispatch(NetworkAction.CheckConnectivity)
//        }?.addTo(compositeDisposable)

        view.loadURL(url!!)
    }
}
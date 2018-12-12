/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.IdRes
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import mozilla.lockbox.R
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.support.Constant

interface NewWebView {
    var menuObserver: Consumer<String?>?
    fun loadUrl(url: String)
}

class WebViewPresenter(
    val view: NewWebView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        view.menuObserver = Consumer { item ->
            item?.let {
                when {
                    item.startsWith(Constant.Faq.uri) ->
                        dispatcher.dispatch(RouteAction.OpenWebsite(Constant.Faq.uri))
                    item.startsWith(Constant.FeedbackLink.uri) ->
                        dispatcher.dispatch(RouteAction.OpenWebsite(Constant.FeedbackLink.uri))
                    else -> log.error("Cannot route from list item.")
                }
            }
        }
    }
}
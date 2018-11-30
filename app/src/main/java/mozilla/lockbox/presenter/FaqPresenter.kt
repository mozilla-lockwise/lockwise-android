/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.view.View
import android.webkit.WebView
import io.reactivex.functions.Consumer
import mozilla.lockbox.R
import mozilla.lockbox.action.FaqAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.support.Constant

interface FaqView {
    var webViewObserver: Consumer<String?>?
    fun loadUrl(url: String)
}

class FaqPresenter(
    val view: FaqView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        dispatcher.dispatch(RouteAction.OpenWebsite(Constant.Faq.uri))
    }
}
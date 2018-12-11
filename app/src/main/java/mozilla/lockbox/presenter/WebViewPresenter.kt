/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.IdRes
import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.support.Constant

interface NewWebView {
    val menuListener: Observable<Int>
}

class WebViewPresenter(
    val view: NewWebView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        view.menuListener
            .subscribe(this::onMenuItem)
            .addTo(compositeDisposable)
    }

    private fun onMenuItem(@IdRes itemId: Int) {
        val uri = when (itemId) {
            R.id.faq_menu_item -> Constant.Faq.uri
            R.id.feedback_menu_item -> Constant.FeedbackLink.uri
            else -> return log.error("Cannot route from list item.")
        }
        dispatcher.dispatch(RouteAction.OpenWebsite(uri))
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.IdRes
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.support.Constant
import java.util.Observable

interface NewWebView {
    var menuObserver: Observable<Int>
    var url: String?
    fun loadUrl(url: String)
}

class WebViewPresenter(
    val view: NewWebView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
//        view.menuObserver
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(view::loadUrl)
//            .addTo(compositeDisposable)
    }

}
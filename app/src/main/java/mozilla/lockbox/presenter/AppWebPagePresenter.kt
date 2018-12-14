/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.functions.Consumer
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface WebPageView {
    var webViewObserver: Consumer<String>?
    fun loadURL(url: String)
}

class AppWebPagePresenter(
    val view: WebPageView,
    val url: String?,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        view.loadURL(url!!)
    }
}
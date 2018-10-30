/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.support.Constant

interface FxALoginView {
    var webViewObserver: Consumer<String?>?
    fun loadURL(url: String)
}

class FxALoginPresenter(
    private val view: FxALoginView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val accountStore: AccountStore = AccountStore.shared
) : Presenter() {
    override fun onViewReady() {
        view.webViewObserver = Consumer { url ->
            url?.let {
                if (url.startsWith(Constant.FxA.redirectUri)) {
                    dispatcher.dispatch(AccountAction.OauthRedirect(url))
                }
            }
        }

        accountStore.loginURL
            .subscribe {
                view.loadURL(it)
            }
            .addTo(compositeDisposable)
    }
}
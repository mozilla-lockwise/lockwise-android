/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.support.Constant

interface FxALoginView {
    var webViewObserver: Consumer<String?>?
    val skipFxAClicks: Observable<Unit>?
    fun loadURL(url: String)
}

@ExperimentalCoroutinesApi
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
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                view.loadURL(it)
            }
            .addTo(compositeDisposable)

        view.skipFxAClicks?.subscribe {
                dispatcher.dispatch(LifecycleAction.UseTestData)
            }?.addTo(compositeDisposable)
    }
}
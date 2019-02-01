/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.net.Uri
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.support.Constant

interface FxALoginView {
    var webViewRedirect: ((url: Uri?) -> Boolean)
    val skipFxAClicks: Observable<Unit>?
    fun handleNetworkError(networkErrorVisibility: Boolean)
    val retryNetworkConnectionClicks: Observable<Unit>
//    var triggerOnboarding: Consumer<Boolean>
    fun loadURL(url: String)
}

@ExperimentalCoroutinesApi
class FxALoginPresenter(
    private val view: FxALoginView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val networkStore: NetworkStore = NetworkStore.shared,
    private val accountStore: AccountStore = AccountStore.shared,
    private val routeStore: RouteStore = RouteStore.shared
) : Presenter() {

    fun isRedirectUri(uri: String?): Boolean = uri?.startsWith(Constant.FxA.redirectUri) ?: false

    private val triggerOnboarding: Consumer<Boolean>
        get() = Consumer { onboarding ->
            if(onboarding == true){
                nextRouteSubject.onNext(RouteAction.Onboarding)
            }
            else {
                // is this needed?
                nextRouteSubject.onNext(RouteAction.SkipOnboarding)
            }
        }

    private var nextRouteSubject = ReplaySubject.createWithSize<RouteAction>(1)
    var nextRoute: Observable<RouteAction> = nextRouteSubject

    override fun onViewReady() {
        routeStore.onboarding
            .subscribe{ this::triggerOnboarding }
            .addTo(compositeDisposable)

        view.webViewRedirect = { url ->
            val urlStr = url?.toString() ?: null
            val result = isRedirectUri(urlStr)
            if (result) {
                dispatcher.dispatch(AccountAction.OauthRedirect(urlStr!!))
            }
            result
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

        networkStore.isConnected
            .subscribe(view::handleNetworkError)
            .addTo(compositeDisposable)

        view.retryNetworkConnectionClicks.subscribe {
            dispatcher.dispatch(NetworkAction.CheckConnectivity)
        }?.addTo(compositeDisposable)
    }
}
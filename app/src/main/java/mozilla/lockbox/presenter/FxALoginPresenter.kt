/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.net.Uri
import android.os.Build
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.Constant

interface FxALoginView {
    var webViewRedirect: ((url: Uri?) -> Boolean)
    val skipFxAClicks: Observable<Unit>?
    //    val retryNetworkConnectionClicks: Observable<Unit>
    fun handleNetworkError(networkErrorVisibility: Boolean)

    fun loadURL(url: String)
}

@ExperimentalCoroutinesApi
class FxALoginPresenter(
    private val view: FxALoginView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val networkStore: NetworkStore = NetworkStore.shared,
    private val accountStore: AccountStore = AccountStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val settingStore: SettingStore = SettingStore.shared
) : Presenter() {
    fun isRedirectUri(uri: String?): Boolean = uri?.startsWith(Constant.FxA.redirectUri) ?: false

    override fun onViewReady() {
        view.webViewRedirect = { url ->
            val urlStr = url?.toString()
            val result = isRedirectUri(urlStr)
            if (result) {
                dispatcher.dispatch(OnboardingStatusAction(true))
                dispatcher.dispatch(AccountAction.OauthRedirect(urlStr!!))
                triggerOnboarding()
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
            dispatcher.dispatch(OnboardingStatusAction(true))
            dispatcher.dispatch(LifecycleAction.UseTestData)
            triggerOnboarding()
        }?.addTo(compositeDisposable)

        networkStore.isConnected
            .subscribe(view::handleNetworkError)
            .addTo(compositeDisposable)

//        view.retryNetworkConnectionClicks.subscribe {
//            dispatcher.dispatch(NetworkAction.CheckConnectivity)
//        }?.addTo(compositeDisposable)
    }

    private fun triggerOnboarding() {
        if (fingerprintStore.isFingerprintAuthAvailable) {
            dispatcher.dispatch(RouteAction.Onboarding.FingerprintAuth)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && settingStore.autofillAvailable) {
            dispatcher.dispatch(RouteAction.Onboarding.Autofill)
        } else {
            dispatcher.dispatch(RouteAction.Onboarding.Confirmation)
        }
    }
}
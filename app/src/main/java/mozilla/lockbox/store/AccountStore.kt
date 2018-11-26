/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asSingle
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.OAuthInfo
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FxAProfile
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.toFxAProfile

private const val FIREFOX_ACCOUNT_KEY = "firefox-account"

@ExperimentalCoroutinesApi
open class AccountStore(
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val securePreferences: SecurePreferences = SecurePreferences.shared
) {
    companion object {
        val shared by lazy { AccountStore() }
    }

    internal val compositeDisposable = CompositeDisposable()

    private val storedAccountJSON: String?
        get() = securePreferences.getString(FIREFOX_ACCOUNT_KEY)

    private var fxa: FirefoxAccount? = null

    open val loginURL: Observable<String> = ReplaySubject.createWithSize(1)
    val oauthInfo: Observable<Optional<OAuthInfo>> = ReplaySubject.createWithSize(1)
    val profile: Observable<Optional<FxAProfile>> = ReplaySubject.createWithSize(1)

    init {
        val resetObservable = this.dispatcher.register
            .filter { it == LifecycleAction.UserReset }
            .map { AccountAction.Reset }

        this.dispatcher.register
            .filterByType(AccountAction::class.java)
            .mergeWith(resetObservable)
            .subscribe {
                when (it) {
                    is AccountAction.OauthRedirect -> {
                        this.oauthLogin(it.url)
                    }
                    is AccountAction.Reset -> {
                        this.clear()
                    }
                }
            }
            .addTo(compositeDisposable)

        storedAccountJSON?.let { accountJSON ->
            this.fxa = FirefoxAccount.fromJSONString(accountJSON)
            generateLoginURL()
            populateAccountInformation()
        } ?: run {
            this.generateNewFirefoxAccount()
        }
    }

    private fun populateAccountInformation() {
        val profileSubject = profile as Subject
        val oauthSubject = oauthInfo as Subject

        fxa?.let { fxa ->
            securePreferences.putString(FIREFOX_ACCOUNT_KEY, fxa.toJSONString())

            fxa.getProfile().asSingle(Dispatchers.Default)
                .subscribe { profile ->
                    profileSubject.onNext(profile.toFxAProfile().asOptional())
                }
                .addTo(compositeDisposable)

            // can't use asSingle here because the OAuthToken is optional :-/
            Observable
                .create<Optional<OAuthInfo>> { emitter ->
                    val oauthDeferred = fxa.getCachedOAuthToken(Constant.FxA.scopes)
                    oauthDeferred.invokeOnCompletion {
                        emitter.onNext(oauthDeferred.getCompleted().asOptional())
                    }
                }
                .subscribe(oauthSubject)
        }
    }

    private fun generateNewFirefoxAccount() {
        Config.release().asSingle(Dispatchers.Default)
            .subscribe { config ->
                fxa = FirefoxAccount(config, Constant.FxA.clientID, Constant.FxA.redirectUri)
                generateLoginURL()
            }
            .addTo(compositeDisposable)

        (oauthInfo as Subject).onNext(Optional(null))
        (profile as Subject).onNext(Optional(null))
    }

    private fun generateLoginURL() {
        fxa?.beginOAuthFlow(Constant.FxA.scopes, true)?.asSingle(Dispatchers.Default)?.subscribe { url ->
            (this.loginURL as Subject).onNext(url)
        }?.addTo(compositeDisposable)
    }

    private fun oauthLogin(url: String) {
        val uri = Uri.parse(url)
        val codeQP = uri.getQueryParameter("code")
        val stateQP = uri.getQueryParameter("state")

        codeQP?.let { code ->
            stateQP?.let { state ->
                fxa?.completeOAuthFlow(code, state)?.asSingle(Dispatchers.Default)?.subscribe { oauthInfo ->
                    (this.oauthInfo as Subject).onNext(oauthInfo.asOptional())
                    this.populateAccountInformation()
                }?.addTo(compositeDisposable)
            }
        }
    }

    private fun clear() {
        CookieManager.getInstance().removeAllCookies { }
        WebStorage.getInstance().deleteAllData()

        this.securePreferences.remove(FIREFOX_ACCOUNT_KEY)
        this.generateNewFirefoxAccount()
    }
}
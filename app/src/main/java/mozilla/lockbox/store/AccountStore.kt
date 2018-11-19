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
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.OAuthInfo
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FxAProfile
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.toFxAProfile

open class AccountStore(
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val securePreferences: SecurePreferences = SecurePreferences.shared
) {
    companion object {
        val shared by lazy { AccountStore() }
    }

    internal val compositeDisposable = CompositeDisposable()

    private val storedAccountJSON: String?
        get() = securePreferences.getString(Constant.Key.firefoxAccount)

    private var fxa: FirefoxAccount? = null

    open val loginURL: Observable<String> = ReplaySubject.createWithSize(1)
    val syncCredentials: Observable<Optional<SyncCredentials>> = ReplaySubject.createWithSize(1)
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
            FirefoxAccount.fromJSONString(accountJSON).whenComplete {
                this.fxa = it
                this.generateLoginURL()
                this.populateAccountInformation()
            }
        } ?: run {
            this.generateNewFirefoxAccount()
        }
    }

    private fun populateAccountInformation() {
        fxa?.toJSONString()?.let {
            securePreferences.putString(Constant.Key.firefoxAccount, it)
        }

        val profileSubject = profile as Subject
        fxa?.getProfile()?.whenComplete {
            profileSubject.onNext(it.toFxAProfile().asOptional())
        }

        val syncCredentialsSubject = syncCredentials as Subject
        fxa?.getOAuthToken(Constant.FxA.scopes)?.whenComplete {
            val credentials = generateSyncCredentials(it)
            syncCredentialsSubject.onNext(credentials.asOptional())
        }
    }

    private fun generateSyncCredentials(oauthInfo: OAuthInfo): SyncCredentials? {
        val fxa = fxa ?: return null
        val tokenServerURL = fxa.getTokenServerEndpointURL() ?: return null
        return SyncCredentials(oauthInfo, tokenServerURL, Constant.FxA.oldSyncScope)
    }

    private fun generateNewFirefoxAccount() {
        Config.release().whenComplete {
            this.fxa = FirefoxAccount(it, Constant.FxA.clientID, Constant.FxA.redirectUri)
            this.generateLoginURL()

            (this.syncCredentials as Subject).onNext(Optional(null))
            (this.profile as Subject).onNext(Optional(null))
        }
    }

    private fun generateLoginURL() {
        this.fxa?.beginOAuthFlow(Constant.FxA.scopes, true)?.whenComplete {
            (this.loginURL as Subject).onNext(it)
        }
    }

    private fun oauthLogin(url: String) {
        val uri = Uri.parse(url)
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")

        code?.let { it ->
            state?.let { state ->
                fxa?.completeOAuthFlow(it, state)?.whenComplete {
                    this.populateAccountInformation()
                }
            }
        }
    }

    private fun clear() {
        CookieManager.getInstance().removeAllCookies { }
        WebStorage.getInstance().deleteAllData()

        this.securePreferences.remove(Constant.Key.firefoxAccount)
        this.generateNewFirefoxAccount()
    }
}
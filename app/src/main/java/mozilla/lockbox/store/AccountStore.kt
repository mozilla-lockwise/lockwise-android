/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.net.Uri
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.OAuthInfo
import mozilla.components.service.fxa.Profile
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional

private const val FIREFOX_ACCOUNT_KEY = "firefox-account"
private val FXA_SCOPES = arrayOf("profile", "https://identity.mozilla.com/apps/lockbox", "https://identity.mozilla.com/apps/oldsync")

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
    val profile: Observable<Optional<Profile>> = ReplaySubject.createWithSize(1)

    init {
        this.dispatcher.register
            .filterByType(AccountAction::class.java)
            .subscribe {
                when (it) {
                    is AccountAction.OauthRedirect -> {
                        this.oauthLogin(it.url)
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
            Config.release().whenComplete {
                this.fxa = FirefoxAccount(it, Constant.FxA.clientID, Constant.FxA.redirectUri)
                this.generateLoginURL()

                (this.oauthInfo as Subject).onNext(Optional(null))
                (this.profile as Subject).onNext(Optional(null))
            }
        }
    }

    private fun populateAccountInformation() {
        fxa?.toJSONString()?.let {
            securePreferences.putString(FIREFOX_ACCOUNT_KEY, it)
        }

        val profileSubject = profile as Subject
        fxa?.getProfile()?.whenComplete {
            profileSubject.onNext(it.asOptional())
        }

        val oauthSubject = oauthInfo as Subject
        fxa?.getOAuthToken(FXA_SCOPES)?.whenComplete {
            oauthSubject.onNext(it.asOptional())
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
}
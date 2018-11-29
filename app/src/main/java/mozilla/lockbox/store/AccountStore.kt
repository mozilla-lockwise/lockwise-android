/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.net.Uri
import android.os.Looper
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
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.model.FxASyncCredentials
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FixedFxAProfile
import mozilla.lockbox.support.FxAProfile
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.toFxAOauthInfo
import mozilla.lockbox.support.toFxAProfile

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
        get() = securePreferences.getString(Constant.Key.firefoxAccount)

    private var fxa: FirefoxAccount? = null

    open val loginURL: Observable<String> = ReplaySubject.createWithSize(1)
    val syncCredentials: Observable<Optional<SyncCredentials>> = ReplaySubject.createWithSize(1)
    val profile: Observable<Optional<FxAProfile>> = ReplaySubject.createWithSize(1)

    init {
        val resetObservable = this.dispatcher.register
            .filter { it == LifecycleAction.UserReset }
            .map { AccountAction.Reset }

        val useTestData = this.dispatcher.register
            .filter { it == LifecycleAction.UseTestData }
            .map { AccountAction.UseTestData }

        this.dispatcher.register
            .filterByType(AccountAction::class.java)
            .mergeWith(resetObservable)
            .mergeWith(useTestData)
            .subscribe {
                when (it) {
                    is AccountAction.OauthRedirect -> this.oauthLogin(it.url)
                    is AccountAction.UseTestData -> this.populateTestAccountInformation(true)
                    is AccountAction.Reset -> this.clear()
                }
            }
            .addTo(compositeDisposable)

        storedAccountJSON?.let { accountJSON ->
            if (accountJSON == Constant.App.testMarker) {
                populateTestAccountInformation(false)
            } else {
                this.fxa = FirefoxAccount.fromJSONString(accountJSON)
                generateLoginURL()
                populateAccountInformation(false)
            }
        } ?: run {
            this.generateNewFirefoxAccount()
        }
    }

    private fun populateTestAccountInformation(isNew: Boolean) {
        val profileSubject = profile as Subject
        val syncCredentialSubject = syncCredentials as Subject

        securePreferences.putString(Constant.Key.firefoxAccount, Constant.App.testMarker)

        profileSubject.onNext(FixedFxAProfile().asOptional())
        syncCredentialSubject.onNext(FixedSyncCredentials(isNew).asOptional())
    }

    private fun populateAccountInformation(isNew: Boolean) {
        val profileSubject = profile as Subject
        val syncCredentialSubject = syncCredentials as Subject

        fxa?.let { fxa ->
            securePreferences.putString(Constant.Key.firefoxAccount, fxa.toJSONString())

            fxa.getProfile().asSingle(Dispatchers.Default)
                .subscribe { profile ->
                    profileSubject.onNext(profile.toFxAProfile().asOptional())
                }
                .addTo(compositeDisposable)

            // can't use asSingle here because the OAuthToken is optional :-/
            Observable
                .create<Optional<SyncCredentials>> { emitter ->
                    val oauthDeferred = fxa.getCachedOAuthToken(Constant.FxA.scopes)
                    oauthDeferred.invokeOnCompletion {
                        val credentials = oauthDeferred.getCompleted()?.let { oauthInfo ->
                            generateSyncCredentials(oauthInfo, isNew)
                        }
                        emitter.onNext(credentials.asOptional())
                    }
                }
                .subscribe(syncCredentialSubject)
        }
    }

    private fun generateSyncCredentials(oauthInfo: OAuthInfo, isNew: Boolean): SyncCredentials? {
        val fxa = fxa ?: return null
        val tokenServerURL = fxa.getTokenServerEndpointURL()
        return FxASyncCredentials(oauthInfo.toFxAOauthInfo(), tokenServerURL, Constant.FxA.oldSyncScope, isNew)
    }

    private fun generateNewFirefoxAccount() {
        Config.release().asSingle(Dispatchers.Default)
            .subscribe { config ->
                fxa = FirefoxAccount(config, Constant.FxA.clientID, Constant.FxA.redirectUri)
                generateLoginURL()
            }
            .addTo(compositeDisposable)
        (syncCredentials as Subject).onNext(Optional(null))
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
                    this.populateAccountInformation(true)
                }?.addTo(compositeDisposable)
            }
        }
    }

    private fun clear() {
        if (Looper.myLooper() != null) {
            CookieManager.getInstance().removeAllCookies { }
            WebStorage.getInstance().deleteAllData()
        }

        this.securePreferences.remove(Constant.Key.firefoxAccount)
        this.generateNewFirefoxAccount()
    }
}
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
import kotlinx.coroutines.rx2.asMaybe
import kotlinx.coroutines.rx2.asSingle
import mozilla.components.service.fxa.AccessTokenInfo
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.FxaException
import mozilla.components.service.fxa.Profile
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.model.FxASyncCredentials
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import java.util.concurrent.TimeUnit

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
    val profile: Observable<Optional<Profile>> = ReplaySubject.createWithSize(1)

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
                try {
                    this.fxa = FirefoxAccount.fromJSONString(accountJSON)
                } catch (e: FxaException) {
                    dispatcher.dispatch(RouteAction.Dialog.NoNetworkDisclaimer)
                }
                generateLoginURL()
                populateAccountInformation(false)
            }
        } ?: run {
            this.generateNewFirefoxAccount()
        }
    }

    private fun populateTestAccountInformation(isNew: Boolean) {
//        val profileSubject = profile as Subject
//        val syncCredentialSubject = syncCredentials as Subject
//
//        securePreferences.putString(Constant.Key.firefoxAccount, Constant.App.testMarker)

//        profileSubject.onNext(FixedFxAProfile().asOptional())
//        syncCredentialSubject.onNext(FixedSyncCredentials(isNew).asOptional())
    }

    private fun populateAccountInformation(isNew: Boolean) {
        val profileSubject = profile as Subject
        val syncCredentialSubject = syncCredentials as Subject
        val fxa = fxa ?: return
        securePreferences.putString(Constant.Key.firefoxAccount, fxa.toJSONString())

        fxa.getProfile()
            .asSingle(Dispatchers.Default)
            .delay(1L, TimeUnit.SECONDS)
            .map { it.asOptional() }
            .subscribe(profileSubject::onNext, this::pushError)
            .addTo(compositeDisposable)

        fxa.getAccessToken(Constant.FxA.oldSyncScope)
            .asMaybe(Dispatchers.Default)
            .delay(1L, TimeUnit.SECONDS)
            .map {
                generateSyncCredentials(it, isNew).asOptional()
            }
            .subscribe(syncCredentialSubject::onNext, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun generateSyncCredentials(oauthInfo: AccessTokenInfo, isNew: Boolean): SyncCredentials? {
        val fxa = fxa ?: return null
        val tokenServerURL = fxa.getTokenServerEndpointURL()
        return FxASyncCredentials(oauthInfo, tokenServerURL, isNew)
    }

    private fun generateNewFirefoxAccount() {
        try {
            val config = Config.release(Constant.FxA.clientID, Constant.FxA.redirectUri)
            fxa = FirefoxAccount(config)
            generateLoginURL()
        } catch (e: FxaException) {
            this.pushError(e)
        }
        (syncCredentials as Subject).onNext(Optional(null))
        (profile as Subject).onNext(Optional(null))
    }

    private fun generateLoginURL() {
        val fxa = fxa ?: return

        fxa.beginOAuthFlow(Constant.FxA.scopes, true)
            .asSingle(Dispatchers.Default)
            .subscribe((this.loginURL as Subject)::onNext, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun oauthLogin(url: String) {
        val fxa = fxa ?: return

        val uri = Uri.parse(url)
        val codeQP = uri.getQueryParameter("code")
        val stateQP = uri.getQueryParameter("state")

        codeQP?.let { code ->
            stateQP?.let { state ->
                fxa.completeOAuthFlow(code, state)
                    .asSingle(Dispatchers.Default)
                    .map { true }
                    .subscribe(this::populateAccountInformation, this::pushError)
                    .addTo(compositeDisposable)
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

    private fun pushError(it: Throwable) {
        dispatcher.dispatch(RouteAction.Dialog.NoNetworkDisclaimer)
    }
}
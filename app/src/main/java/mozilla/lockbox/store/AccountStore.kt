/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asMaybe
import kotlinx.coroutines.rx2.asSingle
import mozilla.appservices.fxaclient.FxaException
import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.components.concept.sync.Avatar
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.concept.sync.Profile
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.model.FxASyncCredentials
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
open class AccountStore(
    private val lifecycleStore: LifecycleStore = LifecycleStore.shared,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val securePreferences: SecurePreferences = SecurePreferences.shared
) : ContextStore {
    companion object {
        val shared by lazy { AccountStore() }
    }

    internal val compositeDisposable = CompositeDisposable()

    private val exceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, e ->
            log.error(
                message = "Unexpected error occurred during Firefox Account authentication",
                throwable = e
            )
        }

    private val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + exceptionHandler

    private val testProfile = Profile(
        uid = "test",
        email = "whovian@tardis.net",
        displayName = "Jodie Whittaker",
        avatar = Avatar(
            url = "https://nerdist.com/wp-content/uploads/2017/11/The-Doctor-Jodie-Whittaker.jpg",
            isDefault = true
        )
    )

    private val storedAccountJSON: String?
        get() = securePreferences.getString(Constant.Key.firefoxAccount)

    private var fxa: FirefoxAccount? = null

    open val loginURL: Observable<String> = ReplaySubject.createWithSize(1)
    private val syncCredentials: Observable<Optional<SyncCredentials>> = ReplaySubject.createWithSize(1)
    open val profile: Observable<Optional<Profile>> = ReplaySubject.createWithSize(1)

    init {
        val resetObservable = lifecycleStore.lifecycleEvents
            .filter { it == LifecycleAction.UserReset }
            .map { AccountAction.Reset }

        val useTestData = lifecycleStore.lifecycleEvents
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

        // Moves credentials from the AccountStore, into the DataStore.
        syncCredentials.map {
            it.value?.let { credentials -> DataStoreAction.UpdateCredentials(credentials) }
                ?: DataStoreAction.Reset
        }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        detectAccount()
    }

    private fun detectAccount() {
        storedAccountJSON?.let { accountJSON ->
            if (accountJSON == Constant.App.testMarker) {
                populateTestAccountInformation(false)
            } else {
                try {
                    this.fxa = FirefoxAccount.fromJSONString(accountJSON)
                } catch (e: FxaException) {
                    pushError(e)
                }
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

        profileSubject.onNext(testProfile.asOptional())
        syncCredentialSubject.onNext(FixedSyncCredentials(isNew).asOptional())
    }

    private fun populateAccountInformation(isNew: Boolean) {
        val profileSubject = profile as Subject
        val syncCredentialSubject = syncCredentials as Subject
        val fxa = fxa ?: return
        securePreferences.putString(Constant.Key.firefoxAccount, fxa.toJSONString())

        fxa.getProfile()
            .asSingle(coroutineContext)
            .delay(1L, TimeUnit.SECONDS)
            .map { it.asOptional() }
            .subscribe(profileSubject::onNext, this::pushError)
            .addTo(compositeDisposable)

        fxa.getAccessToken(Constant.FxA.oldSyncScope)
            .asMaybe(coroutineContext)
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
            .asSingle(coroutineContext)
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
                    .asSingle(coroutineContext)
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
        when (it) {
            is FxaException.Unauthorized -> log.error(
                "FxA error populating account information. Message: " + it.message,
                it
            )
            is FxaException.Unspecified -> log.error("Unspecified FxA error. Message: " + it.message, it)
            is FxaException.Network -> log.error("FxA network error. Message: " + it.message, it)
            is FxaException.Panic -> log.error("FxA error. Message: " + it.message, it)
        }
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
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
import mozilla.components.concept.sync.OAuthScopedKey
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.ServerConfig
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.sharing.AccountSharing
import mozilla.components.service.fxa.sharing.ShareableAccount
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.model.FxASyncCredentials
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.DeviceSystemTimingSupport
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.SystemTimingSupport
import mozilla.lockbox.support.asOptional
import org.json.JSONObject
import java.io.File
import java.lang.Long.min
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
open class AccountStore(
    private val lifecycleStore: LifecycleStore = LifecycleStore.shared,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val securePreferences: SecurePreferences = SecurePreferences.shared,
    private val timingSupport: SystemTimingSupport = DeviceSystemTimingSupport.shared
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

    private lateinit var context: Context

    private val tokenRotationHandler = Handler()

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
                    is AccountAction.AutomaticLogin -> this.automaticLogin(it.account)
                    is AccountAction.Reset -> this.clear()
                }
            }
            .addTo(compositeDisposable)

        // Moves credentials from the AccountStore, into the DataStore.
        syncCredentials
            .map {
                it.value?.let { credentials -> DataStoreAction.UpdateSyncCredentials(credentials) }
                    ?: DataStoreAction.Reset
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        detectAccount()
        this.context = context
    }

    fun shareableAccount(): ShareableAccount? {
        return AccountSharing.queryShareableAccounts(context).firstOrNull()
    }

    private fun automaticLogin(account: ShareableAccount) {
        fxa?.migrateFromSessionTokenAsync(
            account.authInfo.sessionToken,
            account.authInfo.kSync,
            account.authInfo.kXCS
        )
        ?.let {
            it.asSingle(coroutineContext)
                .map { true }
                .subscribe(this::populateAccountInformation, this::pushError)
                .addTo(compositeDisposable)
        }
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

        val fxa = fxa ?: return
        securePreferences.putString(Constant.Key.firefoxAccount, fxa.toJSONString())

        fxa.getProfileAsync()
            .asMaybe(coroutineContext)
            .delay(1L, TimeUnit.SECONDS)
            .map { it.asOptional() }
            .subscribe(profileSubject::onNext, this::pushError)
            .addTo(compositeDisposable)

        val token = securePreferences.getString(Constant.Key.accessToken)?.let {
            accessTokenInfoFromJSON(JSONObject(it))
        }

        token?.let { handleAccessToken(it, isNew) } ?: tokenRotationHandler.post { fetchFreshToken(isNew) }
    }

    private fun fetchFreshToken(isNewLogin: Boolean = false) {
        val fxa = fxa ?: return
        fxa.getAccessTokenAsync(Constant.FxA.oldSyncScope)
            .asMaybe(coroutineContext)
            .delay(1L, TimeUnit.SECONDS)
            .subscribe({ token ->
                handleAccessToken(token, isNewLogin)
            }, this::pushError)
            .addTo(compositeDisposable)
    }

    private fun handleAccessToken(
        token: AccessTokenInfo,
        isNewLogin: Boolean
    ) {
        // We've just got a new token. It might be from secure preferences (we've just been
        // restarted) or a token refresh.
        // 1. Update the rest of the app.
        generateSyncCredentials(token, isNewLogin)
            .take(1)
            .observeOn(mainThread())
            .subscribe((syncCredentials as Subject)::onNext, this::pushError)
            .addTo(compositeDisposable)

        // 2. Store this token in the secure preferences.
        securePreferences.putString(Constant.Key.accessToken, token.toJSONObject().toString())

        // 3. Schedule a token refresh. It may be quite a long time away.
        // Calculate how long before the token expires in milliseconds.
        val msDelay = token.expiresAt * 1000L - timingSupport.currentTimeMillis

        // We'll wait until it's almost expired, leaving at most 10 minutes to fetch the token.
        val refreshMargin = min(msDelay * 95L / 100L, 10 * 60 * 1000L)

        // Wait until the token has nearly expired, and then fetch a new one.
        scheduleFetchFreshToken(msDelay - refreshMargin)
    }

    private fun scheduleFetchFreshToken(msDelay: Long) {
        // If the app is killed, then this will be refreshed when the app is restarted.
        tokenRotationHandler.postDelayed({
            fetchFreshToken(false)
        }, msDelay)
    }

    private fun generateSyncCredentials(oauthInfo: AccessTokenInfo, isNew: Boolean): Observable<Optional<SyncCredentials>> {
        return Observable.just(Unit)
            .observeOn(Schedulers.io())
            .map {
                fxa?.let {
                    val url = it.getTokenServerEndpointURL()
                    FxASyncCredentials(oauthInfo, url, isNew) as SyncCredentials
                }.asOptional()
            }
    }

    private fun generateNewFirefoxAccount() {
        try {
            val config = ServerConfig.release(Constant.FxA.clientID, Constant.FxA.redirectUri)
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

        fxa.beginOAuthFlowAsync(Constant.FxA.scopes)
            .asMaybe(coroutineContext)
            .map { it.url }
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
                fxa.completeOAuthFlowAsync(code, state)
                    .asSingle(coroutineContext)
                    .map { true }
                    .subscribe(this::populateAccountInformation, this::pushError)
                    .addTo(compositeDisposable)
            }
        }
    }

    private fun clear() {
        removeDeviceFromFxA()

        if (Looper.myLooper() != null) {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }

        tokenRotationHandler.removeCallbacksAndMessages(null)

        this.securePreferences.clear()

        this.generateNewFirefoxAccount()

        // Clear down the webview subsystem as best we can.
        // Unfortunately, some of this assumes we have a webview to hand. No matter, we can just
        // create one.
        // This was previously being held from `injectContext` to now, (PR #694).
        // Now, our very rare event (disconnect) is slightly slower and more memory instance,
        // but our frequent event (injectContext) is fast and svelte.
        WebView(context).clearCache(true)

        // Clear the log directories.
        val logDirectory = context.getDir("webview", Context.MODE_PRIVATE)
        clearLogFolder(logDirectory)
    }

    private fun removeDeviceFromFxA() {
        if (fxa != null) {
            fxa!!.disconnectAsync()
                .asSingle(coroutineContext)
                .subscribe()
                .addTo(compositeDisposable)
        } else {
            log.info("FxA is null. No devices to disconnect.")
        }
    }

    private fun clearLogFolder(dir: File) {
        if (dir.isDirectory) {
            try {
                val leveldbDir = dir.listFiles()
                    ?.first { file ->
                        file.name.startsWith("Local")
                    }
                    ?.listFiles()
                    ?.first { file ->
                        file.name.startsWith("leveldb")
                    }
                    ?.listFiles()

                if (leveldbDir == null) {
                    log.error("Failed to clear the log directory; cannot be found.")
                    return
                }

                for (file in leveldbDir) {
                    val logname = file.name
                    if (logname.endsWith(".log")) {
                        file.delete()
                    }
                }
            } catch (exception: Exception) {
                log.error("Failed to clear the directory.", exception)
            }
        }
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

        dispatcher.dispatch(SentryAction(it))
    }
}

private fun AccessTokenInfo.toJSONObject() = JSONObject()
        .put("scope", scope)
        .put("token", token)
        .put("expiresAt", expiresAt)
        .put("key", key?.toJSONObject())

private fun OAuthScopedKey.toJSONObject() = JSONObject()
        .put("kty", kty)
        .put("scope", scope)
        .put("kid", kid)
        .put("k", k)

private fun accessTokenInfoFromJSON(obj: JSONObject): AccessTokenInfo? {
    val scope = obj.optString("scope")
    val token = obj.optString("token")

    if (scope.isEmpty() || token.isEmpty()) {
        return null
    }

    return AccessTokenInfo(
        scope = scope,
        token = token,
        expiresAt = obj.optLong("expiresAt", 0L),
        key = obj.optJSONObject("key")?.let { oauthScopedKeyFromJSON(it) }
    )
}

private fun oauthScopedKeyFromJSON(obj: JSONObject): OAuthScopedKey? {
    val kty = obj.optString("kty")
    val scope = obj.optString("scope")
    val kid = obj.optString("kid")
    val k = obj.optString("k")

    if (kty.isEmpty() || scope.isEmpty() || kid.isEmpty() || k.isEmpty()) {
        return null
    }

    return OAuthScopedKey(
        kty = kty,
        scope = scope,
        kid = kid,
        k = k
    )
}

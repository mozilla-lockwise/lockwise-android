/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.OAuthInfo
import mozilla.components.service.fxa.Profile
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.SecurePreferences
import java.lang.IllegalArgumentException

private const val FIREFOX_ACCOUNT_KEY = "firefox-account"
private val FXA_SCOPES = arrayOf("profile", "https://identity.mozilla.com/apps/lockbox", "https://identity.mozilla.com/apps/oldsync")

class AccountStore(
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val securePreferences: SecurePreferences = SecurePreferences()
) {
    companion object {
        val shared = AccountStore()
    }

    internal val compositeDisposable = CompositeDisposable()

    val oauthInfo: Observable<Optional<OAuthInfo>> = PublishSubject.create()
    val profile: Observable<Optional<Profile>> = PublishSubject.create()

    init {
        loadFxA()
    }

    private fun loadFxA() {
        try {
            securePreferences.getString(FIREFOX_ACCOUNT_KEY)?.let { accountJSON ->
                FirefoxAccount.fromJSONString(accountJSON).whenComplete {
                    persistFxA(it)
                }
            } ?: run {
                pushNullAndReset()
            }
        } catch (error: IllegalArgumentException) {
            pushNullAndReset()
        }
    }

    private fun persistFxA(account: FirefoxAccount) {
        account.toJSONString()?.let {
            securePreferences.putString(FIREFOX_ACCOUNT_KEY, it)
        }

        val profileSubject = profile as PublishSubject
        account.getProfile().whenComplete {
            profileSubject.onNext(it.asOptional())
        }

        val oauthSubject = oauthInfo as PublishSubject
        account.getOAuthToken(FXA_SCOPES).whenComplete {
            oauthSubject.onNext(it.asOptional())
        }
    }

    private fun pushNullAndReset() {
        val profileSubject = profile as PublishSubject
        val oauthSubject = oauthInfo as PublishSubject
        profileSubject.onNext(Optional(null))
        oauthSubject.onNext(Optional(null))

        dispatcher.dispatch(DataStoreAction.Reset)
    }
}
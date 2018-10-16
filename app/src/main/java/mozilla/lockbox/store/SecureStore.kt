/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import android.util.Base64
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import mozilla.components.lib.dataprotect.Keystore
import mozilla.components.service.fxa.OAuthInfo
import mozilla.components.service.fxa.Profile
import mozilla.lockbox.action.SecureAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import java.nio.charset.StandardCharsets

private const val KEYSTORE_LABEL = "lockbox-keystore"
private const val OAUTH_KEY = "firefox-account-oauth-info"
private const val PROFILE_KEY = "firefox-account-profile"

private const val BASE_64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_PADDING

class SecureStore(
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val keystore: Keystore = Keystore(KEYSTORE_LABEL)
) {
    companion object {
        val shared = SecureStore()
    }

    private lateinit var prefs: SharedPreferences
    internal val compositeDisposable = CompositeDisposable()

    val oauthInfo: Observable<OAuthInfo?> = PublishSubject.create()
    val profile: Observable<Profile?> = PublishSubject.create()

    init {
        this.dispatcher.register
            .filterByType(SecureAction::class.java)
            .subscribe {
                when (it) {
                    is SecureAction.OAuthInfo -> {
                        // tbd: how to convert to & from String values for OAuthInfo...
                    }
                    is SecureAction.Profile -> {
                        // tbd: how to convert to & from String values for Profile...
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    fun apply(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences

        loadAll()
    }

    private fun loadAll() {
        load(OAUTH_KEY)?.let {
            // tbd: how to convert to & from String values for OAuthInfo...
        }

        load(PROFILE_KEY)?.let {
            // tbd: how to convert to & from String values for Profile...
        }
    }

    private fun load(key: String): String? {
        return if (prefs.contains(key)) {
            val value = prefs.getString(key, "")
            val encrypted = Base64.decode(value, BASE_64_FLAGS)
            val plain = keystore.decryptBytes(encrypted)

            String(plain, StandardCharsets.UTF_8)
        } else {
            null
        }
    }

    private fun save(key: String, value: String) {
        val editor = prefs.edit()

        val encrypted = keystore.encryptBytes(value.toByteArray(StandardCharsets.UTF_8))
        val data = Base64.encodeToString(encrypted, BASE_64_FLAGS)

        editor.putString(key, data)

        editor.apply()
    }
}
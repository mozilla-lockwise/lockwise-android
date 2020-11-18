/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.PreferenceManager
import mozilla.components.lib.dataprotect.Keystore
import mozilla.lockbox.log
import mozilla.lockbox.store.ContextStore
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

private const val BASE_64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING

open class SecurePreferences(
    private val keystore: Keystore = Keystore(Constant.App.keystoreLabel)
) : ContextStore {

    companion object {
        val shared by lazy { SecurePreferences() }
    }

    private lateinit var prefs: SharedPreferences

    override fun injectContext(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    open fun getString(key: String): String? {
        verifyKey()

        return if (prefs.contains(key)) {
            val value = prefs.getString(key, "")
            val encrypted = Base64.decode(value, BASE_64_FLAGS)

            val plain = try {
                keystore.decryptBytes(encrypted)
            } catch (error: IllegalArgumentException) {
                log.error("IllegalArgumentException exception: ", error)
                null
            } catch (error: GeneralSecurityException) {
                log.error("Decrypt exception: ", error)
                null
            }

            plain.let {
                if (it != null) String(it, StandardCharsets.UTF_8) else null
            }
        } else {
            null
        }
    }

    open fun putString(key: String, value: String) {
        verifyKey()
        val editor = prefs.edit()

        val encrypted = keystore.encryptBytes(value.toByteArray(StandardCharsets.UTF_8))
        val data = Base64.encodeToString(encrypted, BASE_64_FLAGS)

        editor.putString(key, data).apply()
    }

    open fun remove(key: String) {
        val editor = prefs.edit()

        editor.remove(key)

        editor.apply()
    }

    open fun clear() = prefs
            .edit()
            .clear()
            .apply()

    // these methods won't be used until https://github.com/mozilla-lockwise/lockwise-android/issues/165
    // is addressed.
//    open fun createEncryptCipher(): Cipher {
//        verifyKey()
//
//        return keystore.createEncryptCipher()
//    }
//
//    open fun createDecryptCipher(byteArray: ByteArray): Cipher {
//        verifyKey()
//
//        return keystore.createDecryptCipher(byteArray)
//    }

    private fun verifyKey() {
        if (!keystore.available()) {
            keystore.generateKey()
        }
    }
}

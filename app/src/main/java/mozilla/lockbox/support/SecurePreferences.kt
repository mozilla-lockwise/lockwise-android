/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.SharedPreferences
import android.util.Base64
import mozilla.components.lib.dataprotect.Keystore
import java.nio.charset.StandardCharsets

private const val BASE_64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING

open class SecurePreferences(
    private val keystore: Keystore = Keystore(Constant.App.KEYSTORE_LABEL)
) {
    companion object {
        val shared = SecurePreferences()
    }

    private lateinit var prefs: SharedPreferences

    open fun apply(sharedPreferences: SharedPreferences) {
        prefs = sharedPreferences
    }

    @Throws
    open fun getString(key: String): String? {
        verifyKey()

        return if (prefs.contains(key)) {
            val value = prefs.getString(key, "")
            val encrypted = Base64.decode(value, BASE_64_FLAGS)
            try {
                val plain = keystore.decryptBytes(encrypted)
                String(plain, StandardCharsets.UTF_8)
            } catch (error: IllegalArgumentException) {
                throw error
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

    // these methods won't be used until https://github.com/mozilla-lockbox/lockbox-android/issues/165
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
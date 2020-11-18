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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.nio.charset.StandardCharsets
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(Base64::class, Keystore::class, PreferenceManager::class)
class SecurePreferencesTest {
    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val preferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val keystore: Keystore = PowerMockito.mock(Keystore::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    val subject = SecurePreferences(keystore)

    val encodeValue = "oiljksdflj"
    val decodeValue = "something_decoded".toByteArray(StandardCharsets.UTF_8)

    @Before
    fun setUp() {
        mockStatic(Base64::class.java)
        whenCalled(Base64.encodeToString(any(ByteArray::class.java), anyInt())).thenReturn(encodeValue)
        whenCalled(Base64.decode(anyString(), anyInt())).thenReturn(decodeValue)

        whenCalled(editor.putString(anyString(), anyString())).thenReturn(editor)
        whenCalled(preferences.edit()).thenReturn(editor)

        mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
    }

    @Test
    fun `getString, when the keystore is not available & the preferences don't contain the string, generates the key and returns null`() {
        whenCalled(keystore.available()).thenReturn(false)
        val key = "something"
        whenCalled(preferences.contains(key)).thenReturn(false)

        Assert.assertNull(subject.getString(key))
        verify(keystore).generateKey()
    }

    @Test
    fun `getString, when the keystore is not available, decrypting doesn't throw, & the preferences contain the string, generates the key and returns the unencrypted value`() {
        val key = "some_key"
        whenCalled(keystore.available()).thenReturn(false)
        whenCalled(preferences.contains(key)).thenReturn(true)

        val encodedValue = "khulhjkkjkjhhjkdsfsdf"
        val decryptedBytes = "some_fake_data".toByteArray(StandardCharsets.UTF_8)
        whenCalled(keystore.decryptBytes(decodeValue)).thenReturn(decryptedBytes)
        whenCalled(preferences.getString(key, "")).thenReturn(encodedValue)

        Assert.assertEquals(String(decryptedBytes, StandardCharsets.UTF_8), subject.getString(key))
        verify(keystore).generateKey()
    }

    @Test
    fun `getString, when the keystore is not available, decrypting throws an error, & the preferences contain the string, returns null`() {
        val key = "some_key"
        whenCalled(keystore.available()).thenReturn(false)
        whenCalled(preferences.contains(key)).thenReturn(true)

        val encodedValue = "khulhjkkjkjhhjkdsfsdf"
        whenCalled(keystore.decryptBytes(decodeValue)).thenThrow(IllegalArgumentException())
        whenCalled(preferences.getString(key, "")).thenReturn(encodedValue)

        Assert.assertNull(subject.getString(key))
        verify(keystore).generateKey()
    }

    @Test
    fun `getString, when the keystore is available & the preferences don't contain the string, returns null`() {
        whenCalled(keystore.available()).thenReturn(true)
        val key = "something"
        whenCalled(preferences.contains(key)).thenReturn(false)

        Assert.assertNull(subject.getString(key))
    }

    @Test
    fun `getString, when the keystore is available, decrypting doesn't throw, & the preferences contain the string, returns the unencrypted value`() {
        whenCalled(keystore.available()).thenReturn(true)
        val key = "some_key"
        whenCalled(preferences.contains(key)).thenReturn(true)

        val encodedValue = "khulhjkkjkjhhjkdsfsdf"
        val decryptedBytes = "some_fake_data".toByteArray(StandardCharsets.UTF_8)
        whenCalled(keystore.decryptBytes(decodeValue)).thenReturn(decryptedBytes)
        whenCalled(preferences.getString(key, "")).thenReturn(encodedValue)

        Assert.assertEquals(String(decryptedBytes, StandardCharsets.UTF_8), subject.getString(key))
        verify(keystore).decryptBytes(decodeValue)
    }

    @Test
    fun `putString, when the keystore is not available, generates the key and saves the encrypted value`() {
        val key = "some_key"
        val value = "meow"
        whenCalled(keystore.available()).thenReturn(false)

        val encryptedValue = "KJHJKHKLHJJKLHJKLH".toByteArray(StandardCharsets.UTF_8)
        whenCalled(keystore.encryptBytes(value.toByteArray(StandardCharsets.UTF_8))).thenReturn(encryptedValue)

        subject.putString(key, value)
        verify(editor).putString(key, encodeValue)
        verify(editor).apply()
        verify(keystore).generateKey()
    }

    @Test
    fun `putString, when the keystore is available, saves the encrypted value`() {
        val key = "some_key"
        val value = "meow"

        val encryptedValue = "KJHJKHKLHJJKLHJKLH".toByteArray(StandardCharsets.UTF_8)
        whenCalled(keystore.encryptBytes(value.toByteArray(StandardCharsets.UTF_8))).thenReturn(encryptedValue)

        subject.putString(key, value)
        verify(editor).putString(key, encodeValue)
        verify(editor).apply()
    }

    @Test
    fun `remove`() {
        val keyToRemove = "some_key"

        subject.remove(keyToRemove)

        verify(editor).remove(keyToRemove)
        verify(editor).apply()
    }
}
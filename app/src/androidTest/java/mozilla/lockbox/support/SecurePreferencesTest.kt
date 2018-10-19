/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurePreferencesTest {
//    @Mock
//    val editor = Mockito.mock(SharedPreferences.Editor::class.java)
//
//    @Mock
//    val preferences = Mockito.mock(SharedPreferences::class.java)
//
//    @Mock
//    val keystore = Mockito.mock(Keystore::class.java)
//
//    val subject = SecurePreferences(keystore)
//
//    @Before
//    fun setUp() {
//        subject.apply(preferences)
//
//        `when`(preferences.edit()).thenReturn(editor)
//    }
//
//    @Test
//    fun getString_preferencesDoNotContainString_returnsNull() {
//        Assert.assertNull(subject.getString("something"))
//    }
//
//    @Test
//    fun getString_preferencesContainString_returnsUnencryptedValue() {
//        val key = "some_key"
//
//        val encodedValue = Base64.encode("so encrypt much secure".toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING)
//        val decodedEncryptedValue = Base64.decode(encodedValue, Base64.URL_SAFE or Base64.NO_PADDING)
//
//        val decryptedBytes = "some_fake_data".toByteArray(StandardCharsets.UTF_8)
//        `when`(keystore.decryptBytes(decodedEncryptedValue)).thenReturn(decryptedBytes)
//        `when`(preferences.getString(key, "")).thenReturn(String(encodedValue, StandardCharsets.UTF_8))
//
//        Assert.assertEquals(String(decryptedBytes, StandardCharsets.UTF_8), subject.getString(key))
//        verify(keystore).decryptBytes(decodedEncryptedValue)
//    }
//
//    @Test
//    fun putString() {
//        val key = "some_key"
//        val value = "meow"
//
//        val encryptedValue = "KJHJKHKLHJJKLHJKLH".toByteArray(StandardCharsets.UTF_8)
//        `when`(keystore.encryptBytes(value.toByteArray(StandardCharsets.UTF_8))).thenReturn(encryptedValue)
//
//        val encodedValue = Base64.encodeToString(encryptedValue, Base64.URL_SAFE or Base64.NO_PADDING)
//
//        verify(editor.putString(key, encodedValue))
//        verify(editor.apply())
//    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import io.reactivex.observers.TestObserver
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.OAuthInfo
import mozilla.components.service.fxa.Profile
import mozilla.lockbox.action.SecureAction
import mozilla.lockbox.support.Optional
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class SecureStoreTest {
    class FakePreferences(val editor: FakeEditor) : SharedPreferences {

        var stringsForKeys = HashMap<String, String>()

        override fun contains(key: String?): Boolean {
            return stringsForKeys.contains(key)
        }

        override fun getString(key: String?, defValue: String?): String? {
            return stringsForKeys[key]
        }

        override fun edit(): SharedPreferences.Editor {
            return this.editor
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            TODO("not implemented")
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("not implemented")
        }
        override fun getInt(key: String?, defValue: Int): Int {
            TODO("not implemented")
        }
        override fun getAll(): MutableMap<String, *> {
            TODO("not implemented")
        }
        override fun getLong(key: String?, defValue: Long): Long {
            TODO("not implemented")
        }
        override fun getFloat(key: String?, defValue: Float): Float {
            TODO("not implemented")
        }
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            TODO("not implemented")
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("not implemented")
        }
    }

    class FakeEditor : SharedPreferences.Editor {
        var applyCalled = false

        var putStringKey: String? = null
        var putStringValue: String? = null

        override fun apply() {
            applyCalled = true
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            putStringKey = key
            putStringValue = value

            return this
        }

        override fun clear(): SharedPreferences.Editor {
            TODO("not implemented")
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            TODO("not implemented")
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            TODO("not implemented")
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            TODO("not implemented")
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            TODO("not implemented")
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            TODO("not implemented")
        }
        override fun commit(): Boolean {
            TODO("not implemented")
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            TODO("not implemented")
        }
    }

    // Keystore is final, cannot override
//    class FakeKeystore(override val label: String) : Keystore(label) {
//
//    }

    private val editor = FakeEditor()
    private val preferences = FakePreferences(editor)
    private val oauthObserver = TestObserver<Optional<OAuthInfo>>()
    private val profileObserver = TestObserver<Optional<Profile>>()

    val subject = SecureStore()

    @Before
    fun setUp() {
        subject.oauthInfo.subscribe(oauthObserver)
        subject.profile.subscribe(profileObserver)
    }

    @Test
    fun `when sharedPreferences is applied without saved fxa information`() {
        subject.apply(preferences)

        oauthObserver.assertValue(Optional<OAuthInfo>(null))
        profileObserver.assertValue(Optional<Profile>(null))
    }

    @Test
    fun `when sharedPreferences is applied with saved fxa information`() {
        // note: need sample FxA JSON for this test
//        subject.apply(preferences)
//
//        oauthObserver.assertValue(Optional<OAuthInfo>(null))
//        profileObserver.assertValue(Optional<Profile>(null))
    }

    @Test
    fun `receiving FxAAccount SecureAction`() {
        Config.release().whenComplete {
            val account = FirefoxAccount(it, "", "")

            Dispatcher.shared.dispatch(SecureAction.FxAAccount(account))

            // note: need to be able to mock Keystore for this test
        }
    }
}
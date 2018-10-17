/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.SharedPreferences

class SecurePreferencesTest {
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

    // note: testing will be tricky until we can mock the Keystore (see https://github.com/mozilla-mobile/android-components/issues/1072)
}
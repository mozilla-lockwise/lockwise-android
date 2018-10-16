/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test

class SecureStoreTest {
    class FakePreferences(val editor: FakeEditor) : SharedPreferences {
        override fun contains(key: String?): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getString(key: String?, defValue: String?): String? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun edit(): SharedPreferences.Editor {
            return this.editor
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getInt(key: String?, defValue: Int): Int {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getAll(): MutableMap<String, *> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getLong(key: String?, defValue: Long): Long {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getFloat(key: String?, defValue: Float): Float {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class FakeEditor: SharedPreferences.Editor {
        override fun apply() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun clear(): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun commit(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    // Keystore is final, cannot override
//    class FakeKeystore(override val label: String) : Keystore(label) {
//
//    }

    private val editor = FakeEditor()
    private val preferences = FakePreferences(editor)

    val subject = SecureStore()

    @Before
    fun setUp() {

    }

    @Test
    fun `when sharedPreferences is applied`() {
        subject
    }
}
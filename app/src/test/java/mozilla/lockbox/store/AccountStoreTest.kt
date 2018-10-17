/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.SharedPreferences
import io.reactivex.observers.TestObserver
import mozilla.components.service.fxa.OAuthInfo
import mozilla.components.service.fxa.Profile
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.SecurePreferencesTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AccountStoreTest {
    class FakeSecurePreferences : SecurePreferences() {
        var sharedPrefArgument: SharedPreferences? = null

        var getStringValue: String? = null
        var getStringArgument: String? = null

        var putStringKey: String? = null
        var putStringValue: String? = null

        override fun apply(sharedPreferences: SharedPreferences) {
            sharedPrefArgument = sharedPreferences
        }

        override fun getString(key: String): String? {
            getStringArgument = key
            return getStringValue
        }

        override fun putString(key: String, value: String) {
            putStringKey = key
            putStringValue = value
        }
    }

    private val securePreferences = FakeSecurePreferences()
    private val oauthObserver = TestObserver<Optional<OAuthInfo>>()
    private val profileObserver = TestObserver<Optional<Profile>>()

    val subject = AccountStore(securePreferences = securePreferences)

    @Before
    fun setUp() {
        subject.oauthInfo.subscribe(oauthObserver)
        subject.profile.subscribe(profileObserver)
    }

    @Test
    fun `it passes along the when sharedPreferences is applied`() {
        val preferences = SecurePreferencesTest.FakePreferences(SecurePreferencesTest.FakeEditor())

        subject.apply(preferences)

        Assert.assertEquals(preferences, securePreferences.sharedPrefArgument)
    }

    @Test
    fun `it pushes null after applying, when there is no saved fxa information`() {
        val preferences = SecurePreferencesTest.FakePreferences(SecurePreferencesTest.FakeEditor())

        subject.apply(preferences)

        Assert.assertEquals("firefox-account", securePreferences.getStringArgument)

        oauthObserver.assertValue(Optional<OAuthInfo>(null))
        profileObserver.assertValue(Optional<Profile>(null))
    }

    // note: further FxA-related tests on hold until we can use x-compiled Rust code in unit specs
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

class AccountStoreTest {
//    @Mock
//    private val securePreferences = Mockito.mock(SecurePreferences::class.java)!!
//
//    private val oauthObserver = TestObserver<Optional<OAuthInfo>>()
//    private val profileObserver = TestObserver<Optional<Profile>>()
//
//    val subject = AccountStore(securePreferences = securePreferences)
//
//    @Before
//    fun setUp() {
//        subject.oauthInfo.subscribe(oauthObserver)
//        subject.profile.subscribe(profileObserver)
//    }
//
//    @Test
//    fun `it pushes null when there is no saved fxa information`() {
//        verify(securePreferences.getString("firefox-account"))
//
//        oauthObserver.assertValue(Optional<OAuthInfo>(null))
//        profileObserver.assertValue(Optional<Profile>(null))
//    }

    // note: further FxA-related tests on hold until we can use x-compiled Rust code in unit specs
}
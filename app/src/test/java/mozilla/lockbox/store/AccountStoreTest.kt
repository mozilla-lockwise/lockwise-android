/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.concept.sync.Profile
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.SecurePreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(SecurePreferences::class)
class AccountStoreTest {
    @Mock
    private val securePreferences = PowerMockito.mock(SecurePreferences::class.java)

    private val dispatcher = Dispatcher()

    private val oauthObserver = TestObserver<Optional<SyncCredentials>>()
    private val profileObserver = TestObserver<Optional<Profile>>()

    lateinit var subject: AccountStore

    @Before
    fun setUp() {
        PowerMockito.whenNew(SecurePreferences::class.java).withAnyArguments().thenReturn(securePreferences)

//        subject = AccountStore(dispatcher, securePreferences)
//
//        subject.syncCredentials.subscribe(oauthObserver)
//        subject.profile.subscribe(profileObserver)
    }

    @Test
    fun `it pushes null when there is no saved fxa information`() {
//        verify(securePreferences.getString("firefox-account"))
//
//        oauthObserver.assertValue(Optional<SyncCredentials>(null))
//        profileObserver.assertValue(Optional<Profile>(null))
    }

    // note: further FxA-related tests on hold until we can use x-compiled Rust code in unit specs
}
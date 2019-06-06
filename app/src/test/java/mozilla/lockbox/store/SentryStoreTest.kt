/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.sentry.Sentry
import io.sentry.SentryClient
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(Sentry::class)
class SentryStoreTest {
    @Mock
    val sentryClient: SentryClient = PowerMockito.mock(SentryClient::class.java)

    val dispatcher = Dispatcher()
    val subject = SentryStore(dispatcher)

    @Before
    fun setUp() {
        PowerMockito.mockStatic(Sentry::class.java)
        whenCalled(Sentry.getStoredClient()).thenReturn(sentryClient)
    }

    @Test
    fun `sentry actions pass along throwable to stored client`() {
        val throwable = Throwable("uh oh")
        dispatcher.dispatch(SentryAction(throwable))

        verify(sentryClient).sendException(throwable)
    }
}
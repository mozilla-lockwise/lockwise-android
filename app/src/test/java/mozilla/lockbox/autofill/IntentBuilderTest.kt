/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class IntentBuilderTest {

    @Mock
    val fillResponseBuilder: FillResponseBuilder = mock(FillResponseBuilder::class.java)
    val context = RuntimeEnvironment.systemContext

    @Test
    fun `auth intent sender`() {
        IntentBuilder.getAuthIntentSender(context, fillResponseBuilder)

        assertEquals(fillResponseBuilder, IntentBuilder.responseBuilder)
        // don't know a meaningful way to test the sender / intents
    }

    @Test
    fun `search intent sender`() {
        IntentBuilder.getAuthIntentSender(context, fillResponseBuilder)

        assertEquals(fillResponseBuilder, IntentBuilder.responseBuilder)
        // don't know a meaningful way to test the sender / intents
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.content.Intent
import android.view.autofill.AutofillId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class IntentBuilderTest {
    val usernameId = mock(AutofillId::class.java)
    val passwordId = mock(AutofillId::class.java)
    val parsedStructure = ParsedStructure(usernameId, passwordId, "webDomain", "packageName")
    val fillResponseBuilder = FillResponseBuilder(parsedStructure)
    val context = RuntimeEnvironment.systemContext

    @Test
    fun `auth intent sender`() {
        IntentBuilder.getAuthIntentSender(context, fillResponseBuilder)

        // don't know a meaningful way to test the sender / intents
    }

    @Test
    fun `search intent sender`() {
        IntentBuilder.getAuthIntentSender(context, fillResponseBuilder)

        // don't know a meaningful way to test the sender / intents
    }

    @Test
    fun `roundtrip parcelable response builder`() {
        val intent = Intent()
        IntentBuilder.setResponseBuilder(intent, fillResponseBuilder)
        assertEquals(fillResponseBuilder, IntentBuilder.getResponseBuilder(intent))
    }
}
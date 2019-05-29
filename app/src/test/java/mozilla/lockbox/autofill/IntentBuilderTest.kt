/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.content.Context
import android.content.Intent
import android.view.autofill.AutofillId
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class IntentBuilderTest {
    private val usernameId: AutofillId = mock(AutofillId::class.java)
    private val passwordId: AutofillId = mock(AutofillId::class.java)
    private val parsedStructure = ParsedStructure(usernameId, passwordId, "webDomain", "packageName")
    private val fillResponseBuilder = FillResponseBuilder(parsedStructure)
    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `auth intent sender`() {
        IntentBuilder.getAuthIntentSender(context, fillResponseBuilder)

        // don't know a meaningful way to test the sender / intents
    }

    @Test
    fun `search intent sender`() {
        IntentBuilder.getAuthIntentSender(context, fillResponseBuilder)

        // don't know a meaningful way to test the sender / intents
        val intent = Intent()

        IntentBuilder.setResponseBuilder(intent, fillResponseBuilder)
        IntentBuilder.setSearchRequired(intent)

        assertEquals(fillResponseBuilder, IntentBuilder.getResponseBuilder(intent))
        assertTrue(IntentBuilder.isSearchRequired(intent))
    }

    @Test
    fun `roundtrip parcelable response builder`() {
        val intent = Intent()
        IntentBuilder.setResponseBuilder(intent, fillResponseBuilder)
        assertEquals(fillResponseBuilder, IntentBuilder.getResponseBuilder(intent))
        assertFalse(IntentBuilder.isSearchRequired(intent))
    }
}
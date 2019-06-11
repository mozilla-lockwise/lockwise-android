/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.content.Context
import android.view.autofill.AutofillId
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FillResponseBuilderTest {

    @Mock
    val usernameId: AutofillId = mock(AutofillId::class.java)

    @Mock
    val passwordId: AutofillId = mock(AutofillId::class.java)

    val context: Context = ApplicationProvider.getApplicationContext()
    private val parsedStructure = ParsedStructure(usernameId, passwordId, packageName = "mozilla.lockbox")
    val subject = FillResponseBuilder(parsedStructure)

    @Test
    fun `build authentication fill response`() {
        subject.buildAuthenticationFillResponse(context)
    }
}
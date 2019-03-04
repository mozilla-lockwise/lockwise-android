/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.view.autofill.AutofillId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FillResponseBuilderTest {

    @Mock
    val usernameId = mock(AutofillId::class.java)

    @Mock
    val passwordId = mock(AutofillId::class.java)

    val context = RuntimeEnvironment.application.applicationContext
    val parsedStructure = ParsedStructure(usernameId = usernameId, passwordId = passwordId, packageName = "mozilla.lockbox")
    val subject = FillResponseBuilder(parsedStructure)

    @Test
    fun `build authentication fill response`() {
        subject.buildAuthenticationFillResponse(context)
    }
}
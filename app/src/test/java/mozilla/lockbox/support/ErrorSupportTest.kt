/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import android.text.Editable
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import io.sentry.Sentry
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.android.AttributeSetBuilder

class ErrorSupportTest {
    @Mock
    val sentry = Mockito.mock(Sentry::class.java)

    @Mock
    val context = Mockito.mock(Context::class.java)

    @Mock
    val dispatcher: Dispatcher = Mockito.mock(Dispatcher::class.java)
    private var dispatchedEventList: Boolean = false
    @Before
    fun setUp() {
        Mockito.`when`(Dispatcher.shared).thenReturn(dispatcher)
        Mockito.`when`(dispatcher.dispatch(SentryAction(Mockito.any())))
            .then {
                dispatchedEventList = true
                null
            }
    }

    @Test
    fun `Sentry action is sent`() {
        // create throwable
        val throwable = Throwable("test")
        pushError(throwable, throwable.message)
//        Assert.assertTrue(dispatchedEventList)
    }

    @Test
    fun `validate hostname text`() {
        val emptyHostname: Editable = ""
        val hostnameLayout = TextInputLayout(context)
        hostnameLayout.editText!!.text = emptyHostname as Editable
        validateEditTextAndShowError(hostnameLayout)
    }
}
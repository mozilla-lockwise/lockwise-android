/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mozilla.lockbox.support.Constant.Common.emptyString
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenCalled

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ClipboardSupportTest {
    @Mock
    private val context: Context = Mockito.mock(Context::class.java)

    private val clipboardManager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private lateinit var subject: ClipboardSupport

    @Before
    fun setUp() {
        whenCalled(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboardManager)
        subject = ClipboardSupport(context)
    }

    @Test
    fun `pastes to clipboard`() {
        val testValue = "pasted"
        subject.paste("label", testValue)
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")

        Assert.assertEquals(testValue, clip.text)
    }

    @Test
    fun `clears when clipboard matches`() {
        subject.paste("label", "was pasted")

        subject.clear("was pasted")
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")

        Assert.assertEquals(emptyString, clip.text)
    }

    @Test
    fun `doesn't clear when clipboard is different`() {
        subject.paste("label", "was actually pasted")

        subject.clear("was pasted")
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")

        Assert.assertEquals("was actually pasted", clip.text)
    }
}
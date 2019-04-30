/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ClipboardSupportTest {
    private val clipboardManager =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val subject = ClipboardSupport(clipboardManager)

    @Test
    fun `pastes to clipboard`() {
        var testValue = "pasted"
        subject.paste("label", testValue)
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testValue, clip.text)
    }

    @Test
    fun `clears when clipboard matches`() {
        subject.paste("label", "was pasted")

        subject.clear("was pasted", "cleared value")
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals("cleared value", clip.text)
    }

    @Test
    fun `doesn't clear when clipboard is different`() {
        subject.paste("label", "was actually pasted")

        subject.clear("was pasted", "cleared value")
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals("was actually pasted", clip.text)
    }
}
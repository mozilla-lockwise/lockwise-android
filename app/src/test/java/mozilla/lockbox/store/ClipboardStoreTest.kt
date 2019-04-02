/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.flux.Dispatcher
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
class ClipboardStoreTest : DisposingTest() {

    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: ClipboardStore

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    private val clipboardManager =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Before
    fun setUp() {
        whenCalled(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboardManager)
        dispatcher = Dispatcher()
        subject = ClipboardStore(dispatcher)
        subject.injectContext(context)
    }

    @Test
    fun testCopyUsername() {
        val testString = "my_test_string"
        dispatcher.dispatch(ClipboardAction.CopyUsername(testString))
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testString, clip.text)
    }

    @Test
    fun testCopyPassword() {
        val testString = "my_test_password"
        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testString, clip.text)
    }

    @Test
    fun testReplaceDirty() {
        val testString = "my_test_password"

        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))

        val dirty = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testString, dirty.text)

        subject.replaceDirty(testString)
        val clean = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals("", clean.text)
    }

    @Test
    fun testReplaceDirty_negativeTest() {
        val testString = "my_test_password"

        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))

        val dirty = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testString, dirty.text)

        val url = "https://www.mozilla.org"
        clipboardManager.primaryClip = ClipData.newPlainText("url", url)

        subject.replaceDirty(testString)
        val clean = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(url, clean.text)
    }
}
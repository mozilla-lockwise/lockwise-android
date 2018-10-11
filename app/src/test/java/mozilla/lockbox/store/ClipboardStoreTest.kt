/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import junit.framework.Assert.assertEquals
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ClipboardStoreTest : DisposingTest() {

    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: ClipboardStore

    @Before
    fun setUp() {
        dispatcher = Dispatcher()
        subject = ClipboardStore(dispatcher)
        subject.apply(clipboardManager)
    }

    private val clipboardManager =
        RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Test
    fun testCopyUsername() {
        val testString = "my_test_string"
        dispatcher.dispatch(ClipboardAction.CopyUsername(testString))
        val clip = clipboardManager.primaryClip.getItemAt(0)
        assertEquals(testString, clip.text)
    }

    @Test
    fun testCopyPassword() {
        val testString = "my_test_password"
        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))
        val clip = clipboardManager.primaryClip.getItemAt(0)
        assertEquals(testString, clip.text)
    }

    @Test
    fun testReplaceDirty() {
        val testString = "my_test_password"

        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))

        val dirty = clipboardManager.primaryClip.getItemAt(0)
        assertEquals(testString, dirty.text)

        subject.replaceDirty(testString)
        val clean = clipboardManager.primaryClip.getItemAt(0)
        assertEquals("", clean.text)
    }

    @Test
    fun testReplaceDirty_negativeTest() {
        val testString = "my_test_password"

        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))

        val dirty = clipboardManager.primaryClip.getItemAt(0)
        assertEquals(testString, dirty.text)

        val url = "https://www.mozilla.org"
        clipboardManager.primaryClip = ClipData.newPlainText("url", url)

        subject.replaceDirty(testString)
        val clean = clipboardManager.primaryClip.getItemAt(0)
        assertEquals(url, clean.text)
    }
}
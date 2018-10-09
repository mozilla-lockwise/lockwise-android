/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.ClipboardManager
import android.content.Context
import junit.framework.Assert.assertTrue
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
    }

    @Test
    fun testInit() {
        subject.apply(RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
    }

    @Test
    fun testCopyUsername() {
        val testString = "my_test_string"

        subject.apply(RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        dispatcher.dispatch(ClipboardAction.CopyUsername(testString))

        val clipboardManager: ClipboardManager = RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertTrue(clipboardManager.primaryClip.getItemAt(0).text.equals(testString))
    }

    @Test
    fun testCopyPassword() {
        val testString = "my_test_password"

        subject.apply(RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))

        val clipboardManager: ClipboardManager = RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertTrue(clipboardManager.primaryClip.getItemAt(0).text.equals(testString))
    }

    @Test
    fun testReplaceDirty() {
        val testString = "my_test_password"
        val dirtyString = ""

        subject.apply(RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))

        val clipboardManager: ClipboardManager = RuntimeEnvironment.application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertTrue(clipboardManager.primaryClip.getItemAt(0).text.equals(testString))

        ClipboardStore.shared.replaceDirty(dirtyString, testString)
        assertTrue(clipboardManager.primaryClip.getItemAt(0).text.equals(dirtyString))
    }
}
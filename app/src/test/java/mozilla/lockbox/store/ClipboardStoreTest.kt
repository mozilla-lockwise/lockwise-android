/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.SystemTimingSupport
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenCalled

@Ignore("More reliably clear the clipboard (#644)")
class TestSystemTimeSupport : SystemTimingSupport {
    override val systemTimeElapsed: Long = 2000L
    override val currentTimeMillis: Long = 2000L
}

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class ClipboardStoreTest : DisposingTest() {

    private val timeSupport = TestSystemTimeSupport()
    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: ClipboardStore

    @Mock
    val context: Context = Mockito.mock(Context::class.java)
    @Mock
    val alarmManager: AlarmManager = Mockito.mock(AlarmManager::class.java)

    private val clipboardManager =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Before
    fun setUp() {
        whenCalled(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(clipboardManager)
        whenCalled(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        dispatcher = Dispatcher()
        subject = ClipboardStore(dispatcher, timeSupport)
        subject.injectContext(context)
    }

    @Test
    fun testCopyUsername() {
        val testString = "my_test_string"
        dispatcher.dispatch(ClipboardAction.CopyUsername(testString))
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testString, clip.text)
        verify(alarmManager).set(ArgumentMatchers.eq(AlarmManager.ELAPSED_REALTIME),
            ArgumentMatchers.eq(timeSupport.systemTimeElapsed + 60000L),
            any(PendingIntent::class.java))
    }

    @Test
    fun testCopyPassword() {
        val testString = "my_test_password"
        dispatcher.dispatch(ClipboardAction.CopyPassword(testString))
        val clip = clipboardManager.primaryClip?.getItemAt(0) ?: throw AssertionError("PrimaryClip must not be null")
        Assert.assertEquals(testString, clip.text)
        verify(alarmManager).set(ArgumentMatchers.eq(AlarmManager.ELAPSED_REALTIME),
            ArgumentMatchers.eq(timeSupport.systemTimeElapsed + 60000L),
            any(PendingIntent::class.java))
    }
}
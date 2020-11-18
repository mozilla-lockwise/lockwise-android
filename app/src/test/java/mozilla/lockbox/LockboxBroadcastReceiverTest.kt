/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import mozilla.lockbox.support.ClipboardSupport
import mozilla.lockbox.support.Constant
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class)
class LockboxBroadcastReceiverTest {
    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val preferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val context = Mockito.mock(Context::class.java)

    @Mock
    val intent = Mockito.mock(Intent::class.java)

    @Mock
    val clipboardSupport = Mockito.mock(ClipboardSupport::class.java)

    val subject = LockboxBroadcastReceiver { clipboardSupport }

    @Test
    fun `receiving unexpected intents`() {
        subject.onReceive(context, intent)

        verifyZeroInteractions(context)
        verifyZeroInteractions(preferences)
    }

    @Test
    fun `receiving expected intents with a null context`() {
        whenCalled(intent.action).thenReturn("android.intent.action.BOOT_COMPLETED")
        subject.onReceive(null, intent)

        verifyZeroInteractions(context)
        verifyZeroInteractions(preferences)
    }

    @Test
    fun `receiving expected BOOT_COMPLETED with a context object`() {
        whenCalled(intent.action).thenReturn("android.intent.action.BOOT_COMPLETED")
        Mockito.`when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        Mockito.`when`(preferences.edit()).thenReturn(editor)
        PowerMockito.mockStatic(PreferenceManager::class.java)
        Mockito.`when`(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.onReceive(context, intent)

        verify(preferences).edit()
        verify(editor).putLong(Constant.Key.autoLockTimerDate, 0)
        verify(editor).apply()
    }

    @Test
    fun `receiving expected CLEAR_CLIPBOARD with a contet object`() {
        whenCalled(intent.action).thenReturn(Constant.Key.clearClipboardIntent)
        whenCalled(intent.getStringExtra(Constant.Key.clipboardDirtyExtra)).thenReturn("pasted value")

        subject.onReceive(context, intent)
        verify(clipboardSupport).clear("pasted value")
    }
}
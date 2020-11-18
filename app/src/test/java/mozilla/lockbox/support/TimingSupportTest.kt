/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.preference.PreferenceManager
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.Setting
import mozilla.lockbox.store.SettingStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.longThat
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.Math.abs
import org.mockito.Mockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class, SimpleFileReader::class)
class TimingSupportTest {
    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val preferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    private val fileReader = PowerMockito.mock(SimpleFileReader::class.java)

    @Mock
    val settingStore = PowerMockito.mock(SettingStore::class.java)

    var autoLockTimeStub = BehaviorRelay.createDefault(Constant.SettingDefault.autoLockTime)

    private var bootID = ";kl;jjkloi;kljhafshjkadfsmn"

    private val lockingSupport = spy(TestSystemTimingSupport())

    lateinit var subject: TimingSupport

    @Before
    fun setUp() {
        whenCalled(preferences.edit()).thenReturn(editor)
        whenCalled(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        whenCalled(editor.putString(anyString(), anyString())).thenReturn(editor)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        whenCalled(fileReader.readContents(Constant.App.bootIDPath)).thenReturn(bootID)
        PowerMockito.whenNew(SimpleFileReader::class.java).withAnyArguments().thenReturn(fileReader)

        whenCalled(settingStore.autoLockTime).thenReturn(autoLockTimeStub)
        PowerMockito.whenNew(SettingStore::class.java).withAnyArguments().thenReturn(settingStore)

        subject = TimingSupport(settingStore)
        subject.injectContext(context)
        subject.systemTimingSupport = lockingSupport
    }

    @Test
    fun `receiving new autolocktime settings does nothing`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        (settingStore.autoLockTime as Relay).accept(Setting.AutoLockTime.ThirtyMinutes)

        verifyZeroInteractions(preferences)
        verifyZeroInteractions(editor)
    }

    @Test
    fun `storenextautolocktime sets the autolocktime with the default`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        val defaultAutoLock = Constant.SettingDefault.autoLockTime
        subject.storeNextAutoLockTime()

        verify(preferences).edit()

        verify(editor).putLong(
            eq(Constant.Key.autoLockTimerDate),
            longThat { longArg ->
                abs(defaultAutoLock.ms - longArg) < 100
            }
        )

        verify(editor).apply()
    }

    @Test
    fun `storenextsynctime sets the sync time with the default`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        subject.storeNextSyncTime()

        verify(preferences).edit()

        verify(editor).putLong(
            eq(Constant.Key.syncTimerDate),
            eq(Constant.Common.twentyFourHours)
        )

        verify(editor).apply()
    }

    @Test
    fun `storenextautolocktime with never as the autolocktime setting`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        (settingStore.autoLockTime as Relay).accept(Setting.AutoLockTime.Never)
        subject.storeNextAutoLockTime()

        verify(preferences).edit()

        verify(editor).putLong(
            Constant.Key.autoLockTimerDate,
            Long.MAX_VALUE
        )

        verify(editor).apply()
    }

    @Test
    fun `new autolocktime settings are reflected when calling storenextautolocktime`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        val newAutoLock = Setting.AutoLockTime.ThirtyMinutes
        (settingStore.autoLockTime as Relay).accept(newAutoLock)
        subject.storeNextAutoLockTime()

        verify(preferences).edit()

        verify(editor).putLong(
            eq(Constant.Key.autoLockTimerDate),
            longThat { longArg ->
                abs(SystemClock.elapsedRealtime() + newAutoLock.ms - longArg) < 100
            }
        )

        verify(editor).apply()
    }

    @Test
    fun `when the saved autolocktimerdate is later than the current system time`() {
        val currSystemTime: Long = Constant.Common.sixtySeconds + 10000
        mockSavedAutoLockTime(lockingSupport.systemTimeElapsed + 120000)
        Mockito.`when`(lockingSupport.systemTimeElapsed).thenReturn(currSystemTime)

        Assert.assertFalse(subject.shouldLock)
    }

    @Test
    fun `backdate next lock time`() {
        subject.backdateNextLockTime()

        verify(preferences).edit()
        verify(editor).putLong(Constant.Key.autoLockTimerDate, 0)
        verify(editor).apply()
    }

    @Test
    fun `forward date next lock time`() {
        subject.forwardDateNextLockTime()

        verify(preferences).edit()
        verify(editor).putLong(Constant.Key.autoLockTimerDate, Long.MAX_VALUE)
        verify(editor).apply()
    }

    @Test
    fun `when the saved synctimerdate is later than the current system time`() {
        val currSystemTime: Long = Constant.Common.twentyFourHours + 10000
        mockSavedSyncTime(lockingSupport.systemTimeElapsed + Constant.Common.twentyFourHours + 10000)
        Mockito.`when`(lockingSupport.systemTimeElapsed).thenReturn(currSystemTime)

        Assert.assertFalse(subject.shouldSync)
    }

    @Test
    fun `when the saved synctimerdate is earlier than the current system time`() {
        val currSystemTime: Long = Constant.Common.twentyFourHours + 10000
        mockSavedSyncTime(10000)
        Mockito.`when`(lockingSupport.systemTimeElapsed).thenReturn(currSystemTime)

        Assert.assertTrue(subject.shouldSync)
    }

    @Test
    fun `when the saved synctimerdate is later than the current system time by more than the sync interval`() {
        val currSystemTime: Long = 100
        mockSavedSyncTime(Constant.Common.twentyFourHours + 500000)
        Mockito.`when`(lockingSupport.systemTimeElapsed).thenReturn(currSystemTime)

        Assert.assertTrue(subject.shouldSync)
    }

    private fun mockSavedAutoLockTime(autoLockTimerDate: Long) {
        whenCalled(preferences.getLong(eq(Constant.Key.autoLockTimerDate), anyLong())).thenReturn(autoLockTimerDate)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
    }

    private fun mockSavedSyncTime(syncTimerDate: Long) {
        whenCalled(preferences.getLong(eq(Constant.Key.syncTimerDate), anyLong())).thenReturn(syncTimerDate)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.preference.PreferenceManager
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.FixedSyncCredentials
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

class TestLockingSupport : LockingSupport {
    override var systemTimeElapsed: Long = 0L
}

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class, SimpleFileReader::class, AutoLockSupport::class)
class AutoLockSupportTest {
    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val preferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    private val fileReader = PowerMockito.mock(SimpleFileReader::class.java)

    class FakeSettingStore : SettingStore() {
        override var autoLockTime: Observable<Setting.AutoLockTime> =
            BehaviorRelay.createDefault(Constant.SettingDefault.autoLockTime)
    }

    private val settingStore = FakeSettingStore()
    private val dispatcher = Dispatcher()

    private var bootID = ";kl;jjkloi;kljhafshjkadfsmn"

    private val lockRequiredObserver: TestObserver<Boolean> = TestObserver.create()

    private val lockingSupport = spy(TestLockingSupport())

    val subject = AutoLockSupport(
        settingStore = settingStore
    )

    @Before
    fun setUp() {
        whenCalled(preferences.edit()).thenReturn(editor)
        whenCalled(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        whenCalled(editor.putString(anyString(), anyString())).thenReturn(editor)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        whenCalled(fileReader.readContents(Constant.App.bootIDPath)).thenReturn(bootID)
        PowerMockito.whenNew(SimpleFileReader::class.java).withAnyArguments().thenReturn(fileReader)

        subject.injectContext(context)
        subject.lockingSupport = lockingSupport
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
                abs(SystemClock.elapsedRealtime() + defaultAutoLock.ms - longArg) < 100
            }
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
        dispatcher.dispatch(DataStoreAction.UpdateCredentials(FixedSyncCredentials(false)))
        mockSavedAutoLockTime(lockingSupport.systemTimeElapsed + 600000)

        Assert.assertFalse(subject.shouldLock)
    }

    @Test
    fun `foregrounding lifecycle actions when the saved autolocktimerdate is earlier than the current system time`() {
        dispatcher.dispatch(DataStoreAction.UpdateCredentials(FixedSyncCredentials(false)))
        mockSavedAutoLockTime(lockingSupport.systemTimeElapsed - Constant.Common.startUpLockTime)

        Assert.assertTrue(subject.shouldLock)
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

    private fun mockSavedAutoLockTime(autoLockTimerDate: Long) {
        whenCalled(preferences.getLong(anyString(), anyLong())).thenReturn(autoLockTimerDate)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
    }
}

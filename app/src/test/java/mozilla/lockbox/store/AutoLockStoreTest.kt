/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.preference.PreferenceManager
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.support.Constant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.longThat
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.Math.abs
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class)
class AutoLockStoreTest {
    @Mock
    val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)

    @Mock
    val preferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    class FakeSettingStore : SettingStore() {
        override var autoLockTime: Observable<Setting.AutoLockTime> =
            BehaviorRelay.createDefault(Constant.SettingDefault.autoLockTime)
    }

    class FakeLifecycleStore : LifecycleStore() {
        override val lifecycleFilter: Observable<LifecycleAction> = PublishSubject.create()
    }

    private val settingStore = FakeSettingStore()
    private val lifecycleStore = FakeLifecycleStore()

    private val lockRequiredObserver: TestObserver<Boolean> = TestObserver.create()

    val subject = AutoLockStore(
        settingStore = settingStore,
        lifecycleStore = lifecycleStore
    )

    @Before
    fun setUp() {
        whenCalled(preferences.edit()).thenReturn(editor)
        whenCalled(editor.putLong(anyString(), anyLong())).thenReturn(editor)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
        subject.lockRequired.subscribe(lockRequiredObserver)
    }

    @Test
    fun `receiving non-Background lifecycle actions does nothing`() {
        (lifecycleStore.lifecycleFilter as Subject).onNext(LifecycleAction.UserReset)

        verifyZeroInteractions(preferences)
        verifyZeroInteractions(editor)
    }

    @Test
    fun `receiving new autolocktime settings does nothing`() {
        (settingStore.autoLockTime as Relay).accept(Setting.AutoLockTime.ThirtyMinutes)

        verifyZeroInteractions(preferences)
        verifyZeroInteractions(editor)
    }

    @Test
    fun `receiving Background lifecycle actions sets the autolocktime with the default`() {
        val defaultAutoLock = Constant.SettingDefault.autoLockTime
        (lifecycleStore.lifecycleFilter as Subject).onNext(LifecycleAction.Background)

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
    fun `new autolocktime settings are reflected when backgrounding the app`() {
        val newAutoLock = Setting.AutoLockTime.ThirtyMinutes
        (settingStore.autoLockTime as Relay).accept(newAutoLock)
        (lifecycleStore.lifecycleFilter as Subject).onNext(LifecycleAction.Background)

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
    fun `foregrounding lifecycle actions when the saved autolocktimerdate is later than the current system time`() {
        mockAutoLockSavedValue(SystemClock.elapsedRealtime() + 600000)

        (lifecycleStore.lifecycleFilter as Subject).onNext(LifecycleAction.Foreground)

        lockRequiredObserver.assertValue(false)
    }

    @Test
    fun `foregrounding lifecycle actions when the saved autolocktimerdate is earlier than the current system time`() {
        mockAutoLockSavedValue(SystemClock.elapsedRealtime() - 600000)

        (lifecycleStore.lifecycleFilter as Subject).onNext(LifecycleAction.Foreground)

        lockRequiredObserver.assertValue(true)
    }

    private fun mockAutoLockSavedValue(value: Long) {
        whenCalled(preferences.getLong(anyString(), anyLong())).thenReturn(value)
        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
    }
}

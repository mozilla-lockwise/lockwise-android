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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.LockingSupport
import mozilla.lockbox.support.SimpleFileReader
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
import java.util.UUID
import org.mockito.Mockito.`when` as whenCalled

class TestLockingSupport() : LockingSupport {
    override var systemTimeElapsed: Long = 0L
    override var currentBootId: String = UUID.randomUUID().toString()
}

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class, SimpleFileReader::class, AutoLockStore::class)
class AutoLockStoreTest {
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

    class FakeLifecycleStore : LifecycleStore() {
        override val lifecycleEvents: Observable<LifecycleAction> = PublishSubject.create()
    }

    class FakeDataStore : DataStore() {
        override val state: Observable<DataStore.State> = PublishSubject.create()
    }

    private val settingStore = FakeSettingStore()
    private val lifecycleStore = FakeLifecycleStore()
    private val dataStore = FakeDataStore()

    private var bootID = ";kl;jjkloi;kljhafshjkadfsmn"

    private val lockRequiredObserver: TestObserver<Boolean> = TestObserver.create()

    private val lockingSupport = spy(TestLockingSupport())

    val subject = AutoLockStore(
        settingStore = settingStore,
        lifecycleStore = lifecycleStore,
        dataStore = dataStore
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
        subject.lockRequired.subscribe(lockRequiredObserver)
        subject.lockingSupport = lockingSupport
    }

    @Test
    fun `receiving non-Background lifecycle actions does nothing`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.UserReset)

        verifyZeroInteractions(preferences)
        verifyZeroInteractions(editor)
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
    fun `receiving Background lifecycle actions when the datastore isn't already locked sets the autolocktime with the default`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        val defaultAutoLock = Constant.SettingDefault.autoLockTime
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        (dataStore.state as Subject).onNext(DataStore.State.Unlocked)

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
    fun `new autolocktime settings are reflected when backgrounding the app when the datastore isn't already locked`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        val newAutoLock = Setting.AutoLockTime.ThirtyMinutes
        (settingStore.autoLockTime as Relay).accept(newAutoLock)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        (dataStore.state as Subject).onNext(DataStore.State.Unlocked)

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
    fun `receiving Background lifecycle actions when the datastore is locked does nothing`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        (dataStore.state as Subject).onNext(DataStore.State.Locked)

        verifyZeroInteractions(preferences)
        verifyZeroInteractions(editor)
    }

    @Test
    fun `new autolocktime settings are not reflected when backgrounding the app when the datastore is locked`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        val newAutoLock = Setting.AutoLockTime.ThirtyMinutes
        (settingStore.autoLockTime as Relay).accept(newAutoLock)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        (dataStore.state as Subject).onNext(DataStore.State.Locked)

        verifyZeroInteractions(preferences)
        verifyZeroInteractions(editor)
    }

    @Test
    fun `foregrounding lifecycle actions when the saved autolocktimerdate is later than the current system time`() {
        mockSavedValues(SystemClock.elapsedRealtime() + 600000)

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Foreground)

        lockRequiredObserver.assertValue(false)
    }

    @Test
    fun `foregrounding lifecycle actions when the saved autolocktimerdate is earlier than the current system time`() {
        mockSavedValues(SystemClock.elapsedRealtime() - 600000)

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Foreground)

        lockRequiredObserver.assertValue(true)
    }

    @Test
    fun `datastore locking actions force the autolocktimerdate to an older time`() {
        clearInvocations(preferences)
        clearInvocations(editor)
        Dispatcher.shared.dispatch(DataStoreAction.Lock)

        verify(preferences).edit()

        val expectedNewTime = SystemClock.elapsedRealtime() - 1

        verify(editor).putLong(
            eq(Constant.Key.autoLockTimerDate),
            longThat { longArg ->
                abs(expectedNewTime - longArg) < 100
            }
        )

        verify(editor).apply()
    }

    private fun mockSavedValues(
        autoLockTimerDate: Long
    ) {
        whenCalled(preferences.getLong(anyString(), anyLong())).thenReturn(autoLockTimerDate)

        PowerMockito.mockStatic(PreferenceManager::class.java)
        whenCalled(PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(preferences)

        subject.injectContext(context)
    }
}

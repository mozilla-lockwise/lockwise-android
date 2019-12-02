/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.action.TelemetryAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.assertLastValueMatches
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mozilla.telemetry.event.TelemetryEvent
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.whenNew
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class TelemetryStoreTest : DisposingTest() {
    class FakeTelemetryWrapper : TelemetryWrapper() {
        val applySubject: ReplaySubject<Context> = ReplaySubject.create(1)
        val eventsSubject: ReplaySubject<TelemetryEvent> = ReplaySubject.create(1)
        var uploadSubject: BehaviorSubject<Int> = BehaviorSubject.createDefault(0)

        override val ready: Boolean get() = true
        override fun lateinitContext(ctx: Context) {
            applySubject.onNext(ctx)
        }

        override fun recordEvent(event: TelemetryEvent) {
            eventsSubject.onNext(event)
        }

        override fun scheduleUpload() {
            uploadSubject.onNext(1 + uploadSubject.value!!)
        }
    }

    @Mock
    val settingStore = PowerMockito.mock(SettingStore::class.java)

    val sendUsageDataStub = PublishSubject.create<Boolean>()

    private val dispatcher = Dispatcher()
    private val wrapper = FakeTelemetryWrapper()
    lateinit var subject: TelemetryStore

    @Before
    fun setUp() {
        `when`(settingStore.sendUsageData).thenReturn(sendUsageDataStub)
        whenNew(SettingStore::class.java).withAnyArguments().thenReturn(settingStore)

        subject = TelemetryStore(dispatcher, settingStore, wrapper)
    }

    @Test
    fun `apply config`() {
        val applyObserver = createTestObserver<Context>()
        val context: Context = ApplicationProvider.getApplicationContext()
        wrapper.applySubject.subscribe(applyObserver)
        subject.injectContext(context)
        applyObserver.assertValue(context)
    }

    @Test
    fun `action handling`() {
        val eventsObserver = createTestObserver<TelemetryEvent>()
        val uploadObserver = createTestObserver<Int>()
        wrapper.eventsSubject.subscribe(eventsObserver)
        wrapper.uploadSubject.subscribe(uploadObserver)
        subject.register()

        var action: TelemetryAction = LifecycleAction.Foreground
        dispatcher.dispatch(action)
        eventsObserver.assertLastValueMatches {
            it.toJSON() == action.createEvent().toJSON()
        }
        action = LifecycleAction.Background
        dispatcher.dispatch(action)
        eventsObserver.assertLastValueMatches {
            it.toJSON() == action.createEvent().toJSON()
        }
        uploadObserver.assertLastValue(1)
        action = RouteAction.ItemList
        dispatcher.dispatch(action)
        eventsObserver.assertLastValueMatches {
            it.toJSON() == action.createEvent().toJSON()
        }
        action = DataStoreAction.Lock
        dispatcher.dispatch(action)
        eventsObserver.assertLastValueMatches {
            it.toJSON() == action.createEvent().toJSON()
        }
        action = SettingAction.AutoLockTime(Setting.AutoLockTime.OneMinute)
        dispatcher.dispatch(action)
        eventsObserver.assertLastValueMatches {
            it.toJSON() == action.createEvent().toJSON()
        }
    }

    @Test
    fun `no leaks`() {
        val testUsername = "lockie"
        val testPassword = "lockie123"
        var action: TelemetryAction = ClipboardAction.CopyUsername(testUsername)
        dispatcher.dispatch(action)
        var eventJSON = action.createEvent().toJSON()
        Assert.assertFalse("event does not contain the actual username", eventJSON.contains(testUsername))
        action = ClipboardAction.CopyPassword(testPassword)
        dispatcher.dispatch(action)
        eventJSON = action.createEvent().toJSON()
        Assert.assertFalse("event does not contain the actual password", eventJSON.contains(testPassword))
    }
}

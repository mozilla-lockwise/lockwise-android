/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.TelemetryAction
import mozilla.lockbox.action.TelemetryEventMethod
import mozilla.lockbox.action.TelemetryEventObject
import mozilla.lockbox.flux.Dispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.telemetry.event.TelemetryEvent
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class TelemetryStoreTest : DisposingTest() {
    class FakeTelemetryWrapper : TelemetryWrapper() {
        val applySubject: ReplaySubject<Context> = ReplaySubject.create(1)
        val eventsSubject: ReplaySubject<TelemetryEvent> = ReplaySubject.create(1)

        override val ready: Boolean get() = true
        override fun apply(ctx: Context) {
            applySubject.onNext(ctx)
        }
        override fun recordEvent(event: TelemetryEvent) {
            eventsSubject.onNext(event)
        }
    }

    @Test
    fun testApplyConfig() {
        val dispatcher = Dispatcher()
        val wrapper = FakeTelemetryWrapper()
        val subject = TelemetryStore(dispatcher, wrapper)

        val applyObserver = createTestObserver<Context>()
        wrapper.applySubject.subscribe(applyObserver)
        subject.applyContext(RuntimeEnvironment.application)
        applyObserver.assertValue(RuntimeEnvironment.application)
    }

    @Test
    fun testActionHandling() {
        val dispatcher = Dispatcher()
        val wrapper = FakeTelemetryWrapper()
        val subject = TelemetryStore(dispatcher, wrapper)

        val eventsObserver = createTestObserver<TelemetryEvent>()
        wrapper.eventsSubject.subscribe(eventsObserver)

        var action = TelemetryAction(TelemetryEventMethod.foreground, TelemetryEventObject.app, null, null)
        dispatcher.dispatch(action)
        eventsObserver.assertValue {
            it.toJSON() == action.createEvent().toJSON()
        }
    }
}
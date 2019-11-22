/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class GleanTelemetryStoreTest {
    @Mock
    private val settingStore = mock(SettingStore::class.java)
    private val sendUsageDataStub = ReplaySubject.createWithSize<Boolean>(1)

    @Mock
    val telemetryWrapper = object : GleanWrapper() {
        override var uploadEnabled = false
        var channel: String = ""
        override fun initialize(context: Context, channel: String) {
            this.channel = channel
        }
    }

    val dispatcher = Dispatcher()

    val context: Context = ApplicationProvider.getApplicationContext()

    lateinit var subject: GleanTelemetryStore

    @Before
    fun setUp() {
        sendUsageDataStub.onNext(true)
        `when`(settingStore.sendUsageData).thenReturn(sendUsageDataStub)
        subject = GleanTelemetryStore(telemetryWrapper, settingStore)
    }

    @Test
    fun `when context is injected, verify glean is initialized`() {
        subject.injectContext(context)
        assertTrue(telemetryWrapper.uploadEnabled)
    }

    @Test
    fun `when sendUsageData is toggled, verify glean is turned off`() {
        subject.injectContext(context)
        sendUsageDataStub.onNext(false)
        assertFalse(telemetryWrapper.uploadEnabled)

        sendUsageDataStub.onNext(true)
        assertTrue(telemetryWrapper.uploadEnabled)
    }
}

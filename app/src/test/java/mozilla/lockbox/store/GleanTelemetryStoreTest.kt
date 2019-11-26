/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.view.autofill.AutofillManager
import androidx.test.core.app.ApplicationProvider
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
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

    @Test
    fun `ensure upload enabled is called before initialize`() {
        // We spend quite a lot of effort here to convince ourselves that the user's preference
        // for sending usage data is respected before initializing glean.
        // If this test fails, then either we're losing ping data or we're uploading ping data
        // when the user has explicitly said not to.

        // mock all this out for the setting store, so we can use the Rx machinery it uses.
        val context = mock(Context::class.java)
        `when`(context.getSystemService(eq(AutofillManager::class.java))).thenReturn(mock(AutofillManager::class.java))
        val prefs = mock(SharedPreferences::class.java)
        `when`(prefs.contains(eq(SettingStore.Keys.DEVICE_SECURITY_PRESENT))).thenReturn(true)
        `when`(prefs.contains(eq(SettingStore.Keys.SEND_USAGE_DATA))).thenReturn(true)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)

        val fingerprintStore = mock(FingerprintStore::class.java)
        `when`(fingerprintStore.isDeviceSecure).thenReturn(true)

        class DummyGleanWrapper : GleanWrapper() {
            var initializationOrder = arrayListOf<String>()

            override var uploadEnabled: Boolean = false
                set(value) {
                    initializationOrder.add("uploadEnabled")
                    field = value
                }

            override fun initialize(context: Context, channel: String) {
                initializationOrder.add("initialize")
            }
        }

        fun testWithPref(enabled: Boolean) {
            `when`(
                prefs.getBoolean(
                    eq(SettingStore.Keys.SEND_USAGE_DATA),
                    anyBoolean()
                )
            ).thenReturn(enabled)

            val telemetryWrapper = DummyGleanWrapper()

            val settingStore = SettingStore(fingerprintStore = fingerprintStore)
            val gleanTelemetryStore = GleanTelemetryStore(telemetryWrapper, settingStore)

            // These should appear in the same order as they do in `injectContext` in
            // `LockwiseApplication` and `initializeService` in `LockwiseAutofillService`.
            settingStore.injectContext(context)
            gleanTelemetryStore.injectContext(context)

            assertEquals(
                "uploadEnabled, initialize",
                telemetryWrapper.initializationOrder.joinToString(", ")
            )
            assertEquals(enabled, telemetryWrapper.uploadEnabled)
        }

        testWithPref(false)
        testWithPref(true)
    }
}

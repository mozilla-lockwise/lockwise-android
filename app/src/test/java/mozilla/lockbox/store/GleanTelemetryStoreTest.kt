/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.store

import android.content.Context
import android.content.SharedPreferences
import android.view.autofill.AutofillManager
import androidx.test.core.app.ApplicationProvider
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.GleanMetrics.LegacyIds
import mozilla.lockbox.flux.Dispatcher
import mozilla.telemetry.glean.private.UuidMetricType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.net.TelemetryClient
import org.mozilla.telemetry.schedule.TelemetryScheduler
import org.mozilla.telemetry.storage.TelemetryStorage
import org.powermock.api.mockito.PowerMockito
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

    private val clientIdString = "clientId"

    @Mock
    val client: TelemetryClient = Mockito.mock(TelemetryClient::class.java)

    @Mock
    val configuration: TelemetryConfiguration = Mockito.mock(TelemetryConfiguration::class.java)

    @Mock
    val scheduler: TelemetryScheduler = Mockito.mock(TelemetryScheduler::class.java)

    @Mock
    val storage: TelemetryStorage = Mockito.mock(TelemetryStorage::class.java)

    @Mock
    val telemetry = object : Telemetry(configuration, storage, client, scheduler) {
        override fun getClientId(): String {
            return clientIdString
        }
    }

    //    val telemetry: Telemetry = Mockito.mock(Telemetry::class.java)
    @Mock
    val telemetryHolder: TelemetryHolder = Mockito.mock(TelemetryHolder::class.java)

    @Mock
    private val legacyIds = Mockito.mock(LegacyIds::class.java)

    @Mock
    private val clientIdUuid = Mockito.mock(UuidMetricType::class.java)
    val dispatcher = Dispatcher()
    val context: Context = ApplicationProvider.getApplicationContext()

    lateinit var subject: GleanTelemetryStore

    @Before
    fun setUp() {
        sendUsageDataStub.onNext(true)
        Mockito.`when`(TelemetryHolder.set(telemetry)).thenReturn(Unit)
        PowerMockito.mockStatic(TelemetryHolder::class.java)

//        PowerMockito.whenNew(TelemetryHolder::class.java).withAnyArguments().thenReturn(telemetryHolder)
        PowerMockito.whenNew(LegacyIds::class.java).withAnyArguments().thenReturn(legacyIds)
        PowerMockito.whenNew(UuidMetricType::class.java).withAnyArguments().thenReturn(clientIdUuid)

//        `when`(LegacyIds.clientId.set(any())).thenReturn(Unit)
//        `when`(TelemetryHolder.set()).thenReturn(Unit)
        `when`(telemetry.clientId).thenReturn(clientIdString)
        `when`(settingStore.sendUsageData).thenReturn(sendUsageDataStub)

        subject = GleanTelemetryStore(telemetryWrapper, settingStore)
//        subject.injectContext(context)
    }

    @Test
    @Ignore("Needs to be fixed.")
    fun `when context is injected, verify glean is initialized`() {
        TelemetryHolder.set(telemetry)

        subject.injectContext(context)
        assertTrue(telemetryWrapper.uploadEnabled)
    }

    @Test
    @Ignore("Needs to be fixed.")
    fun `when sendUsageData is toggled, verify glean is turned off`() {
        sendUsageDataStub.onNext(false)
        assertFalse(telemetryWrapper.uploadEnabled)

        sendUsageDataStub.onNext(true)
        assertTrue(telemetryWrapper.uploadEnabled)
    }

    @Test
    @Ignore("Needs to be fixed.")
    fun `ensure upload enabled is called before initialize`() {
        // We spend quite a lot of effort here to convince ourselves that the user's preference
        // for sending usage data is respected before initializing glean.
        // If this test fails, then either we're losing ping data or we're uploading ping data
        // when the user has explicitly said not to.

        // mock all this out for the setting store, so we can use the Rx machinery it uses.
        val context = mock(Context::class.java)
        `when`(context.getSystemService(eq(AutofillManager::class.java))).thenReturn(
            mock(
                AutofillManager::class.java
            )
        )
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

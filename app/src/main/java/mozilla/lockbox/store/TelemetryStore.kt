/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

// The 'service-telemetry' library is deprecated and will go away. Until the Glean
// SDK is fully integrated with Lockwise for Android, keep the suppression on. See
// bug 1597623.
@file:Suppress("DEPRECATION")

package mozilla.lockbox.store

import android.content.Context
import androidx.annotation.VisibleForTesting
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.TelemetryAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.net.TelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

open class TelemetryWrapper {
    private var telemetry: Telemetry? = null

    open val ready: Boolean get() = (telemetry != null)

    open fun lateinitContext(ctx: Context) {
        val res = ctx.resources
        val config = TelemetryConfiguration(ctx)
            .setAppName("Lockbox") // intentionally hard-coded for reporting
            .setServerEndpoint(res.getString(R.string.telemetry_server_endpoint))
            .setUpdateChannel(BuildConfig.BUILD_TYPE)
            .setBuildId(BuildConfig.VERSION_CODE.toString())
            .setCollectionEnabled(true)     // gather the data (so we get startup) ...
            .setUploadEnabled(false)        // ... but don't upload it
        val storage = FileTelemetryStorage(config, JSONPingSerializer())
        val client = TelemetryClient(HttpURLConnectionClient())
        val scheduler = JobSchedulerTelemetryScheduler()

        telemetry = Telemetry(config, storage, client, scheduler)
            .addPingBuilder(TelemetryCorePingBuilder(config))
            .addPingBuilder(TelemetryMobileEventPingBuilder(config))

        TelemetryHolder.set(telemetry)
    }

    open fun update(enabled: Boolean) {
        // process any outstanding pings before settings change ...
        scheduleUpload()

        telemetry?.configuration!!.apply {
            isCollectionEnabled = enabled
            isUploadEnabled = enabled
        }
    }

    open fun recordEvent(event: TelemetryEvent) {
        telemetry?.queueEvent(event)
    }

    open fun scheduleUpload() {
        telemetry?.apply {
            queuePing(TelemetryMobileEventPingBuilder.TYPE)
            queuePing(TelemetryCorePingBuilder.TYPE)
            scheduleUpload()
        }
    }
}

open class TelemetryStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val wrapper: TelemetryWrapper = TelemetryWrapper()
) : ContextStore {
    companion object {
        val shared by lazy { TelemetryStore() }
    }

    internal val compositeDisposable = CompositeDisposable()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun register() {
        dispatcher.register
            .filterByType(TelemetryAction::class.java)
            .subscribe {
                if (!wrapper.ready) {
                    return@subscribe
                }
                wrapper.recordEvent(it.createEvent())
                if (it == LifecycleAction.Background) {
                    // upload pings when going into the background ...
                    wrapper.scheduleUpload()
                }
            }
            .addTo(compositeDisposable)
    }

    override fun injectContext(context: Context) {
        register()
        wrapper.lateinitContext(context)
        settingStore
            .sendUsageData
            .subscribe(wrapper::update)
            .addTo(compositeDisposable)
    }
}

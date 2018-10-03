/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.preference.PreferenceManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.TelemetryAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

open class TelemetryFactory {
    open fun createTelemetry(ctx: Context): Telemetry {
        val config = createConfiguration(ctx)
        val storage = createStorage(config)
        val client = createClient()
        val scheduler = createScheduler()

        return Telemetry(config, storage, client, scheduler)
                .addPingBuilder(TelemetryCorePingBuilder(config))
                .addPingBuilder(TelemetryMobileEventPingBuilder(config))
    }

    open fun createConfiguration(ctx: Context): TelemetryConfiguration {
        val res = ctx.resources
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val enabled = prefs.getBoolean(
                res.getString(R.string.setting_key_telemetry),
                res.getBoolean(R.bool.setting_telemetry_default))

        return TelemetryConfiguration(ctx)
                .setAppName(res.getString(R.string.app_label))
                .setServerEndpoint(res.getString(R.string.telemetry_server_endpoint))
                .setUpdateChannel(BuildConfig.BUILD_TYPE)
                .setBuildId(BuildConfig.VERSION_CODE.toString())
                .setCollectionEnabled(enabled)
                .setUploadEnabled(enabled)
    }
    open fun createStorage(config: TelemetryConfiguration) =
            FileTelemetryStorage(config, JSONPingSerializer())
    open fun createClient() = HttpURLConnectionTelemetryClient()
    open fun createScheduler() = JobSchedulerTelemetryScheduler()
}

open class TelemetryStore(
    val dispatcher: Dispatcher = Dispatcher.shared,
    private val telemetryFactory: TelemetryFactory = TelemetryFactory()
) {
    companion object {
        val shared = TelemetryStore()
    }

    internal val compositeDisposable = CompositeDisposable()
    internal var telemetry: Telemetry? = null

    init {
        dispatcher.register
                .filterByType(TelemetryAction::class.java)
                .subscribe {
                    val t = telemetry ?: return@subscribe
                    val evt = TelemetryEvent.create(
                            "action",
                            it.eventMethod.name,
                            it.eventObject.name,
                            it.value
                    )
                    it.extras?.forEach { ex -> evt.extra(ex.key, ex.value.toString()) }
                    t.queueEvent(evt)
                }
                .addTo(compositeDisposable)
    }

    fun applyContext(ctx: Context) {
        telemetry = telemetryFactory.createTelemetry(ctx)
    }
}
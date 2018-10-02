/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import android.preference.PreferenceManager
import io.reactivex.disposables.CompositeDisposable
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.flux.Dispatcher
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryMobileEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage

open class TelemetryStore(val dispatcher: Dispatcher = Dispatcher.shared) {
    companion object {
        val shared = TelemetryStore()
    }

    internal val compositeDisposable = CompositeDisposable()

    init {
        // TODO: register for Telemetry actions
    }

    var appContext: Context? = null
        set(value) {
            field = value
            if (value != null) {
                setupTelemetry()
            }
        }

    private fun setupTelemetry() {
        val ctx = appContext ?: return
        val res = ctx.resources

        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val enabled = prefs.getBoolean(
                res.getString(R.string.setting_key_telemetry),
                res.getBoolean(R.bool.setting_telemetry_default))

        val config = TelemetryConfiguration(ctx)
                .setAppName(res.getString(R.string.app_label))
                .setServerEndpoint(res.getString(R.string.telemetry_server_endpoint))
                .setUpdateChannel(BuildConfig.BUILD_TYPE)
                .setBuildId(BuildConfig.VERSION_CODE.toString())
                .setCollectionEnabled(enabled)
                .setUploadEnabled(enabled)
        val storage = FileTelemetryStorage(config, JSONPingSerializer())
        val client = HttpURLConnectionTelemetryClient()
        val scheduler = JobSchedulerTelemetryScheduler()
        val telemetry = Telemetry(config, storage, client, scheduler)
                .addPingBuilder(TelemetryCorePingBuilder(config))
                .addPingBuilder(TelemetryMobileEventPingBuilder(config))
        TelemetryHolder.set(telemetry)
    }
}
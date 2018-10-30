/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.app.Application
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.lockbox.presenter.ApplicationPresenter
import mozilla.lockbox.store.ClipboardStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.PreferencesStore
import mozilla.lockbox.store.TelemetryStore
import mozilla.lockbox.support.SecurePreferences

val log: Logger = Logger("Lockbox")
class LockboxApplication : Application() {

    private lateinit var presenter: ApplicationPresenter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        Log.addSink(AndroidLogSink())

        // find shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        SettingStore.shared.apply(sharedPreferences)

        // use context for PreferenceManager
        SecurePreferences.shared.apply(PreferenceManager.getDefaultSharedPreferences(this))

        // Watch for application lifecycle and take appropriate actions
        presenter = ApplicationPresenter()
        ProcessLifecycleOwner.get().lifecycle.addObserver(presenter)

        // use context for system service
        ClipboardStore.shared.apply(getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)

        // hook this context into Telemetry
        TelemetryStore.shared.applyContext(this)

        FingerprintStore.shared.applyContext(this)

        // hook this context into SharedPreferences
        PreferencesStore.shared.applyContext(this)


        // Set up Sentry using DSN (client key) from the Project Settings page on Sentry
        val ctx = this.applicationContext
        // Retrieved from environment's local (or bitrise's "Secrets") environment variable
        val sentryDsn: String? = System.getenv("SENTRY_DSN")
        Sentry.init(sentryDsn, AndroidSentryClientFactory(ctx))
    }
}

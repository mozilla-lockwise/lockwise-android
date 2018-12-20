/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.app.Application
import android.arch.lifecycle.ProcessLifecycleOwner
import android.os.Build
import com.squareup.leakcanary.LeakCanary
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.lockbox.presenter.ApplicationPresenter
import mozilla.lockbox.store.AutoLockStore
import mozilla.lockbox.store.ClipboardStore
import mozilla.lockbox.store.ContextStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.store.TelemetryStore
import mozilla.lockbox.support.FixedDataStoreSupport
import mozilla.lockbox.support.FxASyncDataStoreSupport
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.isTesting

sealed class LogProvider {
    companion object {
        val log: Logger = Logger("Log")
    }
}

val log = LogProvider.log

@ExperimentalCoroutinesApi
class LockboxApplication : Application() {

    private lateinit var presenter: ApplicationPresenter

    override fun onCreate() {
        super.onCreate()
        if (leakCanary()) return
        if (isUnitTest()) return
        injectContext()
        setupDataStoreSupport()
        setupLifecycleListener()
        setupSentry()
    }

    private fun setupDataStoreSupport() {
        // this needs to be done after injectContext, as
        // SyncDataStoreSupport needs to find the database
        // path from the context
        val support = if (isTesting()) {
            FixedDataStoreSupport.shared
        } else {
            FxASyncDataStoreSupport.shared
        }
        DataStore.shared.resetSupport(support)
    }

    private fun injectContext() {
        val contextStoreList: List<ContextStore> = listOf(
            SettingStore.shared,
            SecurePreferences.shared,
            FxASyncDataStoreSupport.shared,
            ClipboardStore.shared,
            FingerprintStore.shared,
            AutoLockStore.shared,
            TelemetryStore.shared
        )

        contextStoreList.forEach {
            it.injectContext(this)
        }
    }

    private fun setupSentry() {
        // Set up Sentry using DSN (client key) from the Project Settings page on Sentry
        val ctx = this.applicationContext
        // Retrieved from environment's local (or bitrise's "Secrets") environment variable
        val sentryDsn: String? = System.getenv("SENTRY_DSN")
        Sentry.init(sentryDsn, AndroidSentryClientFactory(ctx))
    }

    private fun setupLifecycleListener() {
        // Watch for application lifecycle and take appropriate actions
        presenter = ApplicationPresenter()
        ProcessLifecycleOwner.get().lifecycle.addObserver(presenter)
    }

    private fun leakCanary(): Boolean {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return true
        }
        LeakCanary.install(this)

        Log.addSink(AndroidLogSink())
        return false
    }

    private fun isUnitTest(): Boolean {
        var device = Build.DEVICE
        var product = Build.PRODUCT
        if (device == null) {
            device = ""
        }

        if (product == null) {
            product = ""
        }
        return device == "robolectric" && product == "robolectric"
    }
}

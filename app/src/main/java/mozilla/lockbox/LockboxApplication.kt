/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox

import android.app.Application
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.squareup.leakcanary.LeakCanary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.Megazord
import mozilla.components.support.rusthttp.RustHttpConfig
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.rustlog.RustLog
import mozilla.lockbox.presenter.ApplicationPresenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.ClipboardStore
import mozilla.lockbox.store.ContextStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.GleanTelemetryStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.store.SentryStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.store.TelemetryStore
import mozilla.lockbox.support.AdjustSupport
import mozilla.lockbox.support.TimingSupport
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FeatureFlags
import mozilla.lockbox.support.FxASyncDataStoreSupport
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.isDebug

sealed class LogProvider {
    companion object {
        val log: Logger = Logger("Lockwise")
    }
}

val log = LogProvider.log

@ExperimentalCoroutinesApi
open class LockboxApplication : Application() {

    open val unitTesting = false

    private lateinit var presenter: ApplicationPresenter

    override fun onCreate() {
        super.onCreate()
        if (leakCanary()) return
        if (isUnitTest()) return
        setupDataStoreSupport()
        injectContext()
        setupLifecycleListener()

        // Adjust Integration
        val appToken = Constant.App.appToken

        val environment = if (isDebug()) {
            AdjustConfig.ENVIRONMENT_SANDBOX
        } else {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        }

        val config = AdjustConfig(this, appToken, environment)
        Adjust.onCreate(config)
        // register instance of the ActivityLifecycleCallbacks class.
        registerActivityLifecycleCallbacks(AdjustSupport.AdjustLifecycleCallbacks())
    }

    private fun setupDataStoreSupport() {
        Megazord.init()
        RustHttpConfig.setClient(lazy { HttpURLConnectionClient() })
        RustLog.enable()

        FxASyncDataStoreSupport.shared.injectContext(this)

        // This list of stores need to be constructed
        // in the given order. e.g. AccountStore dispatches DataStoreActions.
        val orderedStores = listOf(
            DataStore.shared,
            AccountStore.shared,
            TimingSupport.shared,
            LockedStore.shared
        )
        orderedStores.forEach {
            log.info("${it.javaClass.simpleName} initialized")
        }
    }

    private fun injectContext() {
        val contextStoreList: List<ContextStore> = listOf(
            FingerprintStore.shared,
            SettingStore.shared,
            SecurePreferences.shared,
            ClipboardStore.shared,
            NetworkStore.shared,
            TimingSupport.shared,
            AccountStore.shared,
            if (FeatureFlags.USE_GLEAN) GleanTelemetryStore.shared else TelemetryStore.shared,
            SentryStore.shared,
            PublicSuffixSupport.shared
        )

        contextStoreList.forEach {
            it.injectContext(this)
        }
    }

    private fun setupLifecycleListener() {
        // Watch for application lifecycle and take appropriate actions
        presenter = ApplicationPresenter()
        ProcessLifecycleOwner.get().lifecycle.addObserver(presenter)
    }

    private fun leakCanary(): Boolean {
        // disable LeakCanary when unitTesting
        if (isUnitTest()) return false
        else if (LeakCanary.isInAnalyzerProcess(this)) {
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

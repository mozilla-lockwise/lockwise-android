/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.content.Context
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.components.service.glean.Glean
import mozilla.components.service.glean.config.Configuration
import mozilla.lockbox.BuildConfig

open class GleanWrapper {
    open var uploadEnabled: Boolean
        get() = Glean.getUploadEnabled()
        set(value) {
            Glean.setUploadEnabled(value)
        }

    open fun initialize(context: Context, channel: String) {
        Glean.initialize(context, Configuration(channel = channel))
    }
}

class GleanTelemetryStore(
    private val gleanWrapper: GleanWrapper = GleanWrapper(),
    private val settingStore: SettingStore = SettingStore.shared
) : ContextStore {

    companion object {
        val shared by lazy { GleanTelemetryStore() }
    }

    private val compositeDisposable = CompositeDisposable()

    override fun injectContext(context: Context) {
        gleanWrapper.initialize(context, BuildConfig.BUILD_TYPE)

        settingStore.sendUsageData
            .subscribe {
                gleanWrapper.uploadEnabled = it
            }
            .addTo(compositeDisposable)
    }
}

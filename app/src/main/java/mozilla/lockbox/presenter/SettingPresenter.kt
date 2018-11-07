/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.adapter.AppVersionSettingConfiguration
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment

interface SettingView {
    fun updateSettingList(
        settings: List<SettingCellConfiguration>,
        sections: List<SectionedAdapter.Section>
    )
}

class SettingPresenter(
    val view: SettingView,
    private val context: Context,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    private val versionNumber = BuildConfig.VERSION_NAME

    private val autoLockObserver: Consumer<Boolean>
        get() = Consumer { isToggleOn ->
            if (isToggleOn && fingerprintStore.isFingerprintAuthAvailable) {
                dispatcher.dispatch(
                    RouteAction.DialogFragment.FingerprintDialog(
                        context.getString(R.string.enable_fingerprint_dialog_title),
                        context.getString(R.string.enable_fingerprint_dialog_subtitle)
                    )
                )
            }
        }

    private val autoFillObserver: Consumer<Boolean>
        get() = Consumer { }

    private val sendUsageDataObserver: Consumer<Boolean>
        get() = Consumer { newValue ->
            dispatcher.dispatch(SettingAction.SendUsageData(newValue))
        }

    override fun onViewReady() {
        settingStore.onEnablingFingerprint
            .subscribe {
                if (it is FingerprintAuthAction.OnAuthentication) {
                    when (it.authCallback) {
                        is FingerprintAuthDialogFragment.AuthCallback.OnAuth -> dispatcher.dispatch(
                            SettingAction.UnlockWithFingerprint(true)
                        )
                        is FingerprintAuthDialogFragment.AuthCallback.OnError -> {
                            dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
                            updateSettings()
                        }
                    }
                } else {
                    dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
                    updateSettings()
                }
            }
            .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        updateSettings()
    }

    private fun updateSettings() {
        var settings = listOf(
            TextSettingConfiguration(
                title = context.getString(R.string.auto_lock),
                detailText = context.getString(R.string.auto_lock_option)
            ),
            ToggleSettingConfiguration(
                title = context.getString(R.string.autofill),
                subtitle = context.getString(R.string.autofill_summary),
                toggleDriver = Observable.just(true),
                toggleObserver = autoFillObserver
            ),
            ToggleSettingConfiguration(
                title = context.getString(R.string.send_usage_data),
                subtitle = context.getString(R.string.send_usage_data_summary),
                buttonTitle = context.getString(R.string.learn_more),
                toggleDriver = settingStore.sendUsageData,
                toggleObserver = sendUsageDataObserver
            ),
            AppVersionSettingConfiguration(
                text = "App Version: $versionNumber"
            )
        )

        if (fingerprintStore.isFingerprintAuthAvailable) {
            settings = listOf(
                ToggleSettingConfiguration(
                    title = context.getString(R.string.unlock),
                    toggleDriver = settingStore.unlockWithFingerprint,
                    toggleObserver = autoLockObserver
                )
            ) + settings
        }

        val sections = listOf(
            SectionedAdapter.Section(0, context.getString(R.string.security_title)),
            SectionedAdapter.Section(3, context.getString(R.string.support_title))
        )

        view.updateSettingList(settings, sections)
    }
}
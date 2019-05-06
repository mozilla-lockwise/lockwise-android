/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.adapter.AppVersionSettingConfiguration
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.SettingStore

interface SettingView {
    fun updateSettingList(
        settings: List<SettingCellConfiguration>,
        sections: List<SectionedAdapter.Section>
    )
}

class SettingPresenter(
    val view: SettingView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    private val autoLockTimeClickListener: Consumer<Unit>
        get() = Consumer {
            if (fingerprintStore.isDeviceSecure) {
                dispatcher.dispatch(RouteAction.AutoLockSetting)
            } else {
                dispatcher.dispatch(DialogAction.SecurityDisclaimer)
            }
        }

    private val enableFingerprintObserver: Consumer<Boolean>
        get() = Consumer { isToggleOn ->
            if (isToggleOn && fingerprintStore.isFingerprintAuthAvailable) {
                dispatcher.dispatch(SettingAction.UnlockWithFingerprintPendingAuth(true))
                dispatcher.dispatch(
                    RouteAction.DialogFragment.FingerprintDialog(
                        R.string.enable_fingerprint_dialog_title,
                        R.string.enable_fingerprint_dialog_subtitle
                    )
                )
            } else {
                dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
            }
        }

    private val autoFillObserver: Consumer<Boolean>
        @RequiresApi(Build.VERSION_CODES.O)
        get() = Consumer { newValue ->
            dispatcher.dispatch(SettingAction.Autofill(newValue))

            if (newValue) {
                dispatcher.dispatch(RouteAction.SystemSetting(SettingIntent.Autofill))
            }
        }

    private val sendUsageDataObserver: Consumer<Boolean>
        get() = Consumer { newValue ->
            dispatcher.dispatch(SettingAction.SendUsageData(newValue))
        }

    private val learnMoreSendUsageDataObserver: Consumer<Unit>
        get() = Consumer {
            dispatcher.dispatch(AppWebPageAction.Privacy)
        }

    override fun onViewReady() {
        settingStore.onEnablingFingerprint
            .subscribe {
                when (it) {
                    is FingerprintAuthAction.OnSuccess ->
                        dispatcher.dispatch(SettingAction.UnlockWithFingerprint(true))
                    is FingerprintAuthAction.OnError ->
                        dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
                    is FingerprintAuthAction.OnCancel ->
                        dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
                }

                dispatcher.dispatch(SettingAction.UnlockWithFingerprintPendingAuth(false))
            }
            .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        updateSettings()
    }

    private fun updateSettings() {
        var configurationSettings: List<SettingCellConfiguration> = listOf(
            TextSettingConfiguration(
                title = R.string.auto_lock,
                contentDescription = R.string.auto_lock_description,
                detailTextDriver = settingStore.autoLockTime.map { it.stringValue },
                clickListener = autoLockTimeClickListener
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && settingStore.autofillAvailable) {
            configurationSettings += ToggleSettingConfiguration(
                title = R.string.autofill,
                subtitle = R.string.autofill_summary,
                contentDescription = R.string.autofill_description,
                toggleDriver = Observable.just(settingStore.isCurrentAutofillProvider),
                toggleObserver = autoFillObserver
            )
        }

        if (fingerprintStore.isFingerprintAuthAvailable) {
            configurationSettings = listOf(
                ToggleSettingConfiguration(
                    title = R.string.unlock,
                    contentDescription = R.string.fingerprint_description,
                    toggleDriver = Observables.combineLatest(
                        settingStore.unlockWithFingerprintPendingAuth,
                        settingStore.unlockWithFingerprint
                    )
                        .map { unlock -> unlock.first.takeIf { it } ?: unlock.second },
                    toggleObserver = enableFingerprintObserver
                )
            ) + configurationSettings
        }

        val supportSettings = listOf(
            ToggleSettingConfiguration(
                title = R.string.send_usage_data,
                subtitle = R.string.send_usage_data_summary,
                contentDescription = R.string.send_usage_data,
                buttonTitle = R.string.learn_more,
                buttonObserver = learnMoreSendUsageDataObserver,
                toggleDriver = settingStore.sendUsageData,
                toggleObserver = sendUsageDataObserver
            ),
            AppVersionSettingConfiguration(
                title = R.string.app_version_title,
                appVersion = BuildConfig.VERSION_NAME,
                buildNumber = BuildConfig.BITRISE_BUILD_NUMBER,
                contentDescription = R.string.app_version_description
            )
        )

        val sections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title),
            SectionedAdapter.Section(configurationSettings.size, R.string.support_title)
        )

        view.updateSettingList(configurationSettings + supportSettings, sections)
    }
}

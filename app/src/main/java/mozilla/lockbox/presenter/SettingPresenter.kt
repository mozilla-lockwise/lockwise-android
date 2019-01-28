/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.adapter.AppVersionSettingConfiguration
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
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
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    private val versionNumber = BuildConfig.VERSION_NAME

    private val autoLockTimeClickListener: Consumer<Unit>
        get() = Consumer {
            dispatcher.dispatch(RouteAction.AutoLockSetting)
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
                        is FingerprintAuthDialogFragment.AuthCallback.OnAuth ->
                            dispatcher.dispatch(
                                SettingAction.UnlockWithFingerprint(true)
                            )
                        is FingerprintAuthDialogFragment.AuthCallback.OnError -> {
                            dispatcher.dispatch(
                                SettingAction.UnlockWithFingerprint(false)
                            )
                        }
                    }
                } else {
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
        var settings = listOf(
            TextSettingConfiguration(
                title = R.string.auto_lock,
                contentDescription = R.string.auto_lock_description,
                detailTextDriver = settingStore.autoLockTime.map { it.stringValue },
                clickListener = autoLockTimeClickListener
            ),
            /* TODO: enable Autofill setting and increase supportIndex once it's actually implemented
            ToggleSettingConfiguration(
                title = R.string.autofill,
                subtitle = R.string.autofill_summary,
                contentDescription = R.string.autofill_description,
                toggleDriver = Observable.just(true),
                toggleObserver = autoFillObserver
            ),
            */
            ToggleSettingConfiguration(
                title = R.string.send_usage_data,
                subtitle = R.string.send_usage_data_summary,
                contentDescription = R.string.send_usage_data,
                buttonTitle = R.string.learn_more,
                toggleDriver = settingStore.sendUsageData,
                toggleObserver = sendUsageDataObserver
            ),
            AppVersionSettingConfiguration(
                text = "App Version: $versionNumber",
                contentDescription = R.string.app_version_description
            )
        )

        var supportIndex = 1

        if (fingerprintStore.isFingerprintAuthAvailable) {
            settings = listOf(
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
            ) + settings

            supportIndex += 1
        }

        val sections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title),
            SectionedAdapter.Section(supportIndex, R.string.support_title)
        )

        view.updateSettingList(settings, sections)
    }
}

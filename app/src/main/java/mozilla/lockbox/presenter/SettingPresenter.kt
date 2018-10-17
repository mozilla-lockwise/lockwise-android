/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import mozilla.lockbox.adapter.AppVersionSettingConfiguration
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.BuildConfig

interface SettingView {
    fun updateSettingList(
        settings: List<SettingCellConfiguration>,
        sections: List<SectionedAdapter.Section>
    )
}

class SettingPresenter(val view: SettingView) : Presenter() {
    private val versionNumber = BuildConfig.VERSION_NAME
    override fun onViewReady() {
        val settings = listOf(
            ToggleSettingConfiguration(
                title = "Unlock with fingerprint",
                toggle = false
            ),
            TextSettingConfiguration(
                title = "Auto lock",
                detailText = "5 minutes"
            ),
            ToggleSettingConfiguration(
                title = "Autofill",
                subtitle = "Let Firefox Lockbox fill in logins for you",
                toggle = false
            ),
            ToggleSettingConfiguration(
                title = "Send usage data",
                subtitle = "Mozilla strives to only collect what we need to provide and improve Firefox for everyone. ",
                buttonTitle = "Learn more",
                toggle = true
            ),
            AppVersionSettingConfiguration(
                text = "App Version: $versionNumber"
            )
        )

        val sections = listOf(
            SectionedAdapter.Section(0, "Security"),
            SectionedAdapter.Section(3, "Support")
        )

        view.updateSettingList(settings, sections)
    }
}
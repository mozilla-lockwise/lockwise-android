/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import mozilla.lockbox.adapter.AppVersionSettingConfiguration
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R

interface SettingView {
    fun updateSettingList(
        settings: List<SettingCellConfiguration>,
        sections: List<SectionedAdapter.Section>
    )
}

class SettingPresenter(val view: SettingView, val appContext: Context) : Presenter() {
    private val versionNumber = BuildConfig.VERSION_NAME
    private var context = appContext

    override fun onViewReady() {
        val settings = listOf(
            ToggleSettingConfiguration(
                title = context.getString(R.string.unlock),
                toggle = false
            ),
            TextSettingConfiguration(
                title = context.getString(R.string.auto_lock),
                detailText = context.getString(R.string.auto_lock_option)
            ),
            ToggleSettingConfiguration(
                title = context.getString(R.string.autofill),
                subtitle = context.getString(R.string.autofill_summary),
                toggle = false
            ),
            ToggleSettingConfiguration(
                title = context.getString(R.string.send_usage_data),
                subtitle = context.getString(R.string.send_usage_data_summary),
                buttonTitle = context.getString(R.string.learn_more),
                toggle = true
            ),
            AppVersionSettingConfiguration(
                text = "App Version: $versionNumber"
            )
        )

        val sections = listOf(
            SectionedAdapter.Section(0, context.getString(R.string.security_title)),
            SectionedAdapter.Section(3, context.getString(R.string.support_title))
        )

        view.updateSettingList(settings, sections)
    }
}
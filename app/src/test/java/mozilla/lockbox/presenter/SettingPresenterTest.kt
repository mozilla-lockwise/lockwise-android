/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import junit.framework.Assert
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.adapter.AppVersionSettingConfiguration
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.TextSettingConfiguration
import mozilla.lockbox.adapter.ToggleSettingConfiguration
import org.junit.Test

class SettingPresenterTest {

    class SettingViewFake : SettingView {

        var settingItem: List<SettingCellConfiguration>? = null
        var sectionsItem: List<SectionedAdapter.Section>? = null

        override fun updateSettingList(
            settings: List<SettingCellConfiguration>,
            sections: List<SectionedAdapter.Section>
        ) {
            settingItem = settings
            sectionsItem = sections
        }
    }

    private val settingView = SettingViewFake()
    private val subject = SettingPresenter(settingView)
    private val expectedVersionNumber = BuildConfig.VERSION_NAME

    val expectedSettings = listOf(
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
            text = "App Version: $expectedVersionNumber"
        )
    )

    val expectedSections = listOf(
        SectionedAdapter.Section(0, "Security"),
        SectionedAdapter.Section(3, "Support")
    )

    @Test
    fun onViewReadyTest() {
        subject.onViewReady()

        Assert.assertEquals(settingView.settingItem!![0].title, expectedSettings[0].title)
        Assert.assertEquals(settingView.settingItem!![1].title, expectedSettings[1].title)
        Assert.assertEquals(settingView.settingItem!![2].title, expectedSettings[2].title)
        Assert.assertEquals(settingView.settingItem!![3].title, expectedSettings[3].title)
        Assert.assertEquals(settingView.settingItem!![4].title, expectedSettings[4].title)

        Assert.assertEquals(settingView.sectionsItem!![0].title, expectedSections[0].title)
        Assert.assertEquals(settingView.sectionsItem!![1].title, expectedSections[1].title)
    }
}
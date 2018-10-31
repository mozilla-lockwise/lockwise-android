/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import junit.framework.Assert.assertEquals
import mozilla.lockbox.R
import mozilla.lockbox.adapter.ListAdapterTestHelper
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
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

    private lateinit var context: Context
    private lateinit var testHelper: ListAdapterTestHelper
    private val settingView = SettingViewFake()
    private val subject = SettingPresenter(settingView, RuntimeEnvironment.application.applicationContext)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        testHelper = ListAdapterTestHelper(context)
    }

    @Test
    fun onViewReadyTest() {

        val expectedSettings = testHelper.createAccurateListOfSettings()

        val expectedSections = listOf(
            SectionedAdapter.Section(0, context.getString(R.string.security_title)),
            SectionedAdapter.Section(3, context.getString(R.string.support_title))
        )

        subject.onViewReady()

        assertEquals(settingView.settingItem!![0].title, expectedSettings[0].title)
        assertEquals(settingView.settingItem!![1].title, expectedSettings[1].title)
        assertEquals(settingView.settingItem!![2].title, expectedSettings[2].title)
        assertEquals(settingView.settingItem!![3].title, expectedSettings[3].title)
        assertEquals(settingView.settingItem!![4].title, expectedSettings[4].title)

        assertEquals(settingView.sectionsItem!![0].title, expectedSections[0].title)
        assertEquals(settingView.sectionsItem!![1].title, expectedSections[1].title)
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import mozilla.lockbox.R
import mozilla.lockbox.adapter.SectionedAdapter.Section
import mozilla.lockbox.view.ToggleSettingViewHolder
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class SectionedAdapterTest {

    private val settingAdapter = SettingListAdapter()
    private lateinit var context: Context
    private lateinit var parent: RecyclerView
    private lateinit var testHelper: ListAdapterTestHelper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
        testHelper = ListAdapterTestHelper(context)
    }

    @Test
    fun getItemViewTypeTest() {
        val sections = listOf(
            Section(0, context.getString(R.string.security_title)),
            Section(1, context.getString(R.string.support_title))
        )

        val settings = testHelper.createListOfSettings()
        settingAdapter.setItems(settings)

        val subject = testHelper.createSectionedAdapter(settingAdapter)
        subject.setSections(sections)

        Assert.assertEquals(SectionedAdapter.SECTION_TYPE, subject.getItemViewType(0))
        Assert.assertEquals(SettingListAdapter.SETTING_TOGGLE_TYPE + 1, subject.getItemViewType(1))
        Assert.assertEquals(SectionedAdapter.SECTION_TYPE, subject.getItemViewType(2))
        Assert.assertEquals(SettingListAdapter.SETTING_TEXT_TYPE + 1, subject.getItemViewType(3))
    }

    @Test
    fun getItemCountTest() {
        val sections = listOf(
            Section(0, context.getString(R.string.security_title)),
            Section(3, context.getString(R.string.support_title))
        )

        val settings = testHelper.createListOfSettings()
        settingAdapter.setItems(settings)

        val subject = testHelper.createSectionedAdapter(settingAdapter)
        subject.setSections(sections)

        val count = subject.getItemCount()
        Assert.assertEquals(4, count)
    }

    @Test
    fun isSectionHeaderPositionTest() {
        val sections = listOf(
            Section(0, context.getString(R.string.security_title)),
            Section(3, context.getString(R.string.support_title))
        )
        val settings = testHelper.createListOfSettings()

        settingAdapter.setItems(settings)
        val subject = testHelper.createSectionedAdapter(settingAdapter)
        subject.setSections(sections)

        val headerPosition = subject.isSectionHeaderPosition(0)
        val nonHeaderPosition = subject.isSectionHeaderPosition(1)

        Assert.assertEquals(true, headerPosition)
        Assert.assertEquals(false, nonHeaderPosition)
    }

    @Test
    fun onCreateViewHolderTest_HeaderSection() {
        val sections = listOf(
            Section(0, context.getString(R.string.security_title)),
            Section(3, context.getString(R.string.support_title))
        )

        val subject = testHelper.createSectionedAdapter(
            settingAdapter,
            R.layout.list_cell_setting_header,
            R.id.headerTitle
        )

        subject.setSections(sections)

        val viewHolder = subject.onCreateViewHolder(
            parent = parent,
            typeView = SectionedAdapter.SECTION_TYPE
        )

        assertThat(viewHolder, instanceOf(SectionedAdapter.SectionViewHolder::class.java))
    }

    @Test
    fun onCreateViewHolderTest_ChildViewHolderElement() {
        val sections = listOf(
            Section(0, context.getString(R.string.security_title)),
            Section(3, context.getString(R.string.support_title))
        )

        val settings = testHelper.createListOfSettings()
        settingAdapter.setItems(settings)

        val subject = testHelper.createSectionedAdapter(
            settingAdapter,
            R.layout.list_cell_setting_header,
            R.id.headerTitle
        )

        subject.setSections(sections)

        val viewHolder = subject.onCreateViewHolder(
            parent = parent,
            typeView = SettingListAdapter.SETTING_TOGGLE_TYPE + 1
        )

        assertThat(viewHolder, instanceOf(ToggleSettingViewHolder::class.java))
    }

    @Test
    fun onBindViewHolderTest_HeaderSection() {
        val securityTitle = context.getString(R.string.security_title)

        val sections = listOf(
            Section(0, securityTitle)
        )

        val subject = testHelper.createSectionedAdapter(
            settingAdapter,
            R.layout.list_cell_setting_header,
            R.id.headerTitle
        )
        subject.setSections(sections)

        val viewHolder = subject.onCreateViewHolder(
            parent = parent,
            typeView = SectionedAdapter.SECTION_TYPE
        ) as SectionedAdapter.SectionViewHolder
        subject.onBindViewHolder(sectionViewHolder = viewHolder, position = 0)

        Assert.assertEquals(securityTitle, viewHolder.title.text)
    }

    @Test
    fun onBindViewHolderTest_ChildViewHolderElement() {
        val settingsTitle = context.getString(R.string.unlock)

        val sections = listOf(
            SectionedAdapter.Section(0, context.getString(R.string.security_title))
        )
        val settings = testHelper.createListOfSettings()
        settingAdapter.setItems(settings)

        val subject = testHelper.createSectionedAdapter(
            settingAdapter,
            R.layout.list_cell_setting_header,
            R.id.headerTitle
        )

        subject.setSections(sections)

        val viewHolder = subject.onCreateViewHolder(
            parent = parent,
            typeView = SettingListAdapter.SETTING_TOGGLE_TYPE + 1
        ) as ToggleSettingViewHolder

        subject.onBindViewHolder(sectionViewHolder = viewHolder, position = 1)

        Assert.assertEquals(settingsTitle, viewHolder.title)
    }
}

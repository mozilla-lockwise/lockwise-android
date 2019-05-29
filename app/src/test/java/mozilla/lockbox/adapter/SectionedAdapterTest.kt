/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
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
        context = ApplicationProvider.getApplicationContext()
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
        testHelper = ListAdapterTestHelper()
    }

    @Test
    fun getItemViewTypeTest() {
        val sections = listOf(
            Section(0, R.string.configuration_title),
            Section(1, R.string.support_title)
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
            Section(0, R.string.configuration_title),
            Section(3, R.string.support_title)
        )

        val settings = testHelper.createListOfSettings()
        settingAdapter.setItems(settings)

        val subject = testHelper.createSectionedAdapter(settingAdapter)
        subject.setSections(sections)

        val count = subject.itemCount
        Assert.assertEquals(4, count)
    }

    @Test
    fun isSectionHeaderPositionTest() {
        val sections = listOf(
            Section(0, R.string.configuration_title),
            Section(3, R.string.support_title)
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
            Section(0, R.string.configuration_title),
            Section(3, R.string.support_title)
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
            Section(0, R.string.configuration_title),
            Section(3, R.string.support_title)
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
        val configurationTitle = R.string.configuration_title
        val supportTitle = R.string.support_title

        val sections = listOf(
            Section(0, configurationTitle),
            Section(3, supportTitle)
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
        val marginTopFirst = context.resources.getDimensionPixelSize(R.dimen.section_first_title_top_margin)
        val marginTop = context.resources.getDimensionPixelSize(R.dimen.section_title_top_margin)

        subject.onBindViewHolder(sectionViewHolder = viewHolder, position = 0)

        Assert.assertEquals(context.getString(configurationTitle), viewHolder.title.text)
        Assert.assertEquals(marginTopFirst, (viewHolder.title.layoutParams as LinearLayout.LayoutParams).topMargin)

        subject.onBindViewHolder(sectionViewHolder = viewHolder, position = 4)

        Assert.assertEquals(context.getString(supportTitle), viewHolder.title.text)
        Assert.assertEquals(marginTop, (viewHolder.title.layoutParams as LinearLayout.LayoutParams).topMargin)
    }

    @Test
    fun onBindViewHolderTest_ChildViewHolderElement() {
        val settingsTitle = R.string.unlock

        val sections = listOf(
            SectionedAdapter.Section(0, R.string.configuration_title)
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

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

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
    }

    @Test
    fun getItemViewTypeTest() {
        val sections = listOf(
            SectionedAdapter.Section(0, "Security"),
            SectionedAdapter.Section(1, "Support")
        )

        val settings = listOf(
            ToggleSettingConfiguration("Unlock with fingerprint", toggle = false),
            TextSettingConfiguration("Auto lock", detailText = "5 minutes")
        )
        settingAdapter.setItems(settings)
        val subject = SectionedAdapter(
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            sectionTitleId = 0, sectionLayoutId = 0
        )
        subject.setSections(sections)

        Assert.assertEquals(SectionedAdapter.SECTION_TYPE, subject.getItemViewType(0))
        Assert.assertEquals(SettingListAdapter.SETTING_TOGGLE_TYPE + 1, subject.getItemViewType(1))
        Assert.assertEquals(SectionedAdapter.SECTION_TYPE, subject.getItemViewType(2))
        Assert.assertEquals(SettingListAdapter.SETTING_TEXT_TYPE + 1, subject.getItemViewType(3))
    }

    @Test
    fun getItemCountTest() {
        val sections = listOf(
            SectionedAdapter.Section(0, "Security"),
            SectionedAdapter.Section(3, "Support")
        )

        val settings = listOf(
            ToggleSettingConfiguration("Unlock with fingerprint", toggle = false),
            TextSettingConfiguration("Auto lock", detailText = "5 minutes")
        )
        settingAdapter.setItems(settings)
        val subject = SectionedAdapter(
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            sectionTitleId = 0, sectionLayoutId = 0
        )

        subject.setSections(sections)
        val count = subject.getItemCount()
        Assert.assertEquals(4, count)
    }

    @Test
    fun isSectionHeaderPositionTest() {
        val sections = listOf(
            SectionedAdapter.Section(0, "Security"),
            SectionedAdapter.Section(3, "Support")
        )

        val settings = listOf(
            ToggleSettingConfiguration("Unlock with fingerprint", toggle = false),
            TextSettingConfiguration("Auto lock", detailText = "5 minutes")
        )
        settingAdapter.setItems(settings)
        val subject = SectionedAdapter(
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            sectionTitleId = 0, sectionLayoutId = 0
        )

        subject.setSections(sections)
        val item1 = subject.isSectionHeaderPosition(0)
        val item2 = subject.isSectionHeaderPosition(1)

        Assert.assertEquals(true, item1)
        Assert.assertEquals(false, item2)
    }

    @Test
    fun onCreateViewHolderTest_HeaderSection() {
        val sections = listOf(
            SectionedAdapter.Section(0, "Security"),
            SectionedAdapter.Section(3, "Support")
        )

        val subject = SectionedAdapter(
            R.layout.list_cell_setting_header,
            R.id.headerTitle,
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
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
            SectionedAdapter.Section(0, "Security"),
            SectionedAdapter.Section(3, "Support")
        )

        val settings = listOf(
            ToggleSettingConfiguration("Unlock with fingerprint", toggle = false),
            TextSettingConfiguration("Auto lock", detailText = "5 minutes")
        )
        settingAdapter.setItems(settings)

        val subject = SectionedAdapter(
            R.layout.list_cell_setting_header,
            R.id.headerTitle,
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
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
        val title1 = "Security"
        val sections = listOf(
            SectionedAdapter.Section(0, title1)
        )

        val subject = SectionedAdapter(
            R.layout.list_cell_setting_header,
            R.id.headerTitle,
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        )
        subject.setSections(sections)

        val viewHolder = subject.onCreateViewHolder(
            parent = parent,
            typeView = SectionedAdapter.SECTION_TYPE
        ) as SectionedAdapter.SectionViewHolder
        subject.onBindViewHolder(sectionViewHolder = viewHolder, position = 0)

        Assert.assertEquals(title1, viewHolder.title.text)
    }

    @Test
    fun onBindViewHolderTest_ChildViewHolderElement() {
        val sections = listOf(
            SectionedAdapter.Section(0, "Security")
        )
        val settingsTitle = "Unlock with fingerprint"
        val settings = listOf(
            ToggleSettingConfiguration(title = settingsTitle, toggle = false)
        )
        settingAdapter.setItems(settings)

        val subject = SectionedAdapter(
            R.layout.list_cell_setting_header,
            R.id.headerTitle,
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
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

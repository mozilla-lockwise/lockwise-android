/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_APP_VERSION_TYPE
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_TEXT_TYPE
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_TOGGLE_TYPE
import mozilla.lockbox.view.TextSettingViewHolder
import mozilla.lockbox.view.AppVersionSettingViewHolder
import mozilla.lockbox.view.ToggleSettingViewHolder
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class SettingListAdapterTest {

    val subject = SettingListAdapter()
    private lateinit var context: Context
    private lateinit var parent: RecyclerView

    class SettingCellConfigFake : SettingCellConfiguration(
        title = "Fake title",
        subtitle = "I will fail")

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
    }

    @Test
    fun getItemCountTest() {
        val sectionConfig = listOf(
                TextSettingConfiguration(title = "Auto Lock", detailText = "one hour"),
                ToggleSettingConfiguration(title = "Unlock with finger", toggle = true),
                AppVersionSettingConfiguration(text = "App Version: 1.0")
        )

        subject.setItems(sectionConfig)

        Assert.assertEquals(subject.itemCount, 3)
    }

    @Test
    fun getItemViewTypeTest_WithValidViewTypes() {
        val sectionConfig = listOf(
                TextSettingConfiguration(title = "Auto Lock", detailText = "one hour"),
                ToggleSettingConfiguration(title = "Unlock with finger", toggle = true),
                AppVersionSettingConfiguration(text = "App Version: 1.0")
        )

        subject.setItems(sectionConfig)

        Assert.assertEquals(subject.getItemViewType(0), SETTING_TEXT_TYPE)
        Assert.assertEquals(subject.getItemViewType(1), SETTING_TOGGLE_TYPE)
        Assert.assertEquals(subject.getItemViewType(2), SETTING_APP_VERSION_TYPE)
    }

    @Test
    fun getItemViewTypeTest_WithInvalidViewType() {
        val sectionConfig = listOf(
            SettingCellConfigFake()
        )
        subject.setItems(sectionConfig)

        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            subject.getItemViewType(0)
        }

        val expected = "Please use a valid defined setting type."
        Assertions.assertEquals(expected, exception.message)
    }

    @Test
    fun onCreateViewHolderTest_textSettingCell() {
        val textViewHolder = subject.onCreateViewHolder(parent, 0)
        assertThat(textViewHolder, instanceOf(TextSettingViewHolder::class.java))
    }

    @Test
    fun onCreateViewHolderTest_toggleSettingCell() {
        val toggleViewHolder = subject.onCreateViewHolder(parent, 1)
        assertThat(toggleViewHolder, instanceOf(ToggleSettingViewHolder::class.java))
    }

    @Test
    fun onCreateViewHolderTest_appVersionSettingCell() {
        val toggleViewHolder = subject.onCreateViewHolder(parent, 2)
        assertThat(toggleViewHolder, instanceOf(AppVersionSettingViewHolder::class.java))
    }

    @Test
    fun onCreateViewHolderTest_WithInvalidViewType() {
        val exception = Assertions.assertThrows(IllegalStateException::class.java) {
            subject.onCreateViewHolder(parent, 5)
        }

        val expected = "Please use a valid defined setting type."
        Assertions.assertEquals(expected, exception.message)
    }

    @Test
    fun onBindViewHolderTest_textSettingCell() {
        val title = "Auto Lock"
        val detailText = "one hour"
        val sectionConfig = listOf(
            TextSettingConfiguration(title = title, detailText = detailText),
            ToggleSettingConfiguration(title = "Unlock with finger", toggle = true),
            AppVersionSettingConfiguration(text = "App Version: 1.0")
        )

        subject.setItems(sectionConfig)

        val textViewHolder = subject.onCreateViewHolder(parent, 0) as TextSettingViewHolder

        subject.onBindViewHolder(textViewHolder, 0)

        Assert.assertEquals(title, textViewHolder.title)
        Assert.assertEquals(detailText, textViewHolder.detailText)
    }

    @Test
    fun onBindViewHolderTest_toggleSettingCell() {
        val title = "Unlock with finger"
        val toggleValue = true
        val buttonTitle = "Learn more"
        val sectionConfig = listOf(
            TextSettingConfiguration(title = "Auto Lock", detailText = "one hour"),
            ToggleSettingConfiguration(title = title, toggle = toggleValue, buttonTitle = buttonTitle),
            AppVersionSettingConfiguration(text = "App Version: 1.0")
        )

        subject.setItems(sectionConfig)

        val toggleViewHolder = subject.onCreateViewHolder(parent, 1)
            as ToggleSettingViewHolder

        subject.onBindViewHolder(toggleViewHolder, 1)

        Assert.assertEquals(title, toggleViewHolder.title)
        Assert.assertEquals(toggleValue, toggleViewHolder.toggle)
        Assert.assertEquals(buttonTitle, toggleViewHolder.buttonTitle)
    }

    @Test
    fun onBindViewHolderTest_appVersionSettingCell() {
        val appVersion = "App Version: 1.0"
        val sectionConfig = listOf(
            AppVersionSettingConfiguration(text = appVersion)
        )

        subject.setItems(sectionConfig)

        val appVersionViewHolder = subject.onCreateViewHolder(parent, 2)
            as AppVersionSettingViewHolder

        subject.onBindViewHolder(appVersionViewHolder, 0)

        Assert.assertEquals(appVersion, appVersionViewHolder.text)
    }
}

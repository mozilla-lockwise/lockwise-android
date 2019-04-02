/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_TEXT_TYPE
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_TOGGLE_TYPE
import mozilla.lockbox.view.AppVersionSettingViewHolder
import mozilla.lockbox.view.TextSettingViewHolder
import mozilla.lockbox.view.ToggleSettingViewHolder
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class SettingListAdapterTest {

    val subject = SettingListAdapter()
    private lateinit var context: Context
    private lateinit var parent: RecyclerView
    private lateinit var testHelper: ListAdapterTestHelper
    private val expectedVersionNumber = BuildConfig.VERSION_NAME
    private val expectedBuildNumber = BuildConfig.BITRISE_BUILD_NUMBER

    class SettingCellConfigFake : SettingCellConfiguration(
        title = R.string.search_menu_title,
        subtitle = R.string.cancel,
        contentDescription = R.string.empty_string
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        parent = RecyclerView(context)
        parent.layoutManager = LinearLayoutManager(context)
        testHelper = ListAdapterTestHelper()
    }

    @Test
    fun getItemCountTest() {
        val sectionConfig = testHelper.createListOfSettings()
        val size = sectionConfig.size
        subject.setItems(sectionConfig)
        Assert.assertEquals(subject.itemCount, size)
    }

    @Test
    fun getItemViewTypeTest_WithValidViewTypes() {
        val sectionConfig = testHelper.createListOfSettings()

        subject.setItems(sectionConfig)

        Assert.assertEquals(subject.getItemViewType(0), SETTING_TOGGLE_TYPE)
        Assert.assertEquals(subject.getItemViewType(1), SETTING_TEXT_TYPE)
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
        val expectedTitle = R.string.auto_lock
        val expectedDetailText = R.string.five_minutes

        val sectionConfig = testHelper.createListOfSettings()

        subject.setItems(sectionConfig)

        val textViewHolder = subject.onCreateViewHolder(parent, 0)
            as TextSettingViewHolder

        subject.onBindViewHolder(textViewHolder, 1)

        Assert.assertEquals(expectedTitle, textViewHolder.title)
        Assert.assertEquals(expectedDetailText, textViewHolder.detailTextRes)
    }

    @Test
    fun onBindViewHolderTest_toggleSettingCell() {
        val expectedTitle = context.getString(R.string.unlock)
        val expectedToggleValue = false

        val sectionConfig = testHelper.createListOfSettings()

        subject.setItems(sectionConfig)

        val toggleViewHolder = subject.onCreateViewHolder(parent, 1)
            as ToggleSettingViewHolder

        subject.onBindViewHolder(toggleViewHolder, 0)

        Assert.assertEquals(expectedTitle, context.getString(toggleViewHolder.title))
        Assert.assertEquals(expectedToggleValue, toggleViewHolder.toggle.isChecked)
    }

    @Test
    fun onBindViewHolderTest_appVersionSettingCell() {
        val expectedTitle = "App Version: $expectedVersionNumber ($expectedBuildNumber)"

        val sectionConfig = listOf(
            AppVersionSettingConfiguration(
                title = R.string.app_version_title,
                appVersion = expectedVersionNumber,
                buildNumber = BuildConfig.BITRISE_BUILD_NUMBER,
                contentDescription = R.string.empty_string
            )
        )

        subject.setItems(sectionConfig)

        val appVersionViewHolder = subject.onCreateViewHolder(parent, 2)
            as AppVersionSettingViewHolder

        subject.onBindViewHolder(appVersionViewHolder, 0)

        Assert.assertEquals(expectedTitle, appVersionViewHolder.title)
    }
}

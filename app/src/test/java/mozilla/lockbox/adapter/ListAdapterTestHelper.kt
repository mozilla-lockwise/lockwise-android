/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.TestConsumer
import org.robolectric.annotation.Config

@Config(packageName = "mozilla.lockbox")
class ListAdapterTestHelper {
    private val textDriverFake = Observable.just(R.string.five_minutes)
    private val toggleDriverFake = Observable.just(false)
    private val toggleObserverFake = TestObserver<Boolean>()
    private val toggleConsumerFake = TestConsumer(toggleObserverFake) as Consumer<Boolean>
    private val textClicksObserverFake = TestObserver<Unit>()
    private val textClicksConsumerFake = TestConsumer(toggleObserverFake) as Consumer<Unit>
    private val expectedVersionNumber = BuildConfig.VERSION_NAME

    fun createListOfSettings(): List<SettingCellConfiguration> {

        return listOf(
            ToggleSettingConfiguration(
                title = R.string.unlock,
                contentDescription = R.string.empty_string,
                toggleDriver = toggleDriverFake,
                toggleObserver = toggleConsumerFake
            ),
            TextSettingConfiguration(
                title = R.string.auto_lock,
                contentDescription = R.string.empty_string,
                detailTextDriver = textDriverFake,
                clickListener = textClicksConsumerFake
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun createSectionedAdapter(
        settingAdapter: SettingListAdapter,
        sectionLayoutId: Int = 0,
        sectionTitleId: Int = 0
    ): SectionedAdapter {
        return SectionedAdapter(
            baseAdapter = settingAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>,
            sectionLayoutId = sectionLayoutId,
            sectionTitleId = sectionTitleId
        )
    }

    fun createAccurateListOfSettings(isFingerprintAvailable: Boolean): List<SettingCellConfiguration> {
        var settings = listOf(
            TextSettingConfiguration(
                title = R.string.auto_lock,
                contentDescription = R.string.empty_string,
                detailTextDriver = textDriverFake,
                clickListener = textClicksConsumerFake
            ),
            /* TODO: enable Autofill setting once it's actually implemented
            ToggleSettingConfiguration(
                title = R.string.autofill,
                subtitle = R.string.autofill_summary,
                contentDescription = R.string.empty_string,
                toggleDriver = toggleDriverFake,
                toggleObserver = toggleConsumerFake
            ),
            */
            ToggleSettingConfiguration(
                title = R.string.send_usage_data,
                subtitle = R.string.send_usage_data_summary,
                contentDescription = R.string.empty_string,
                buttonTitle = R.string.learn_more,
                toggleDriver = toggleDriverFake,
                toggleObserver = toggleConsumerFake
            ),
            AppVersionSettingConfiguration(
                text = "App Version: $expectedVersionNumber",
                contentDescription = R.string.empty_string
            )
        )
        if (isFingerprintAvailable) {
            settings = listOf(
                ToggleSettingConfiguration(
                    title = R.string.unlock,
                    contentDescription = R.string.empty_string,
                    toggleDriver = toggleDriverFake,
                    toggleObserver = toggleConsumerFake
                )
            ) + settings
        }
        return settings
    }
}
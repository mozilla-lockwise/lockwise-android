/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.TestConsumer
import org.robolectric.annotation.Config

@Config(packageName = "mozilla.lockbox")
class ListAdapterTestHelper(ctx: Context) {
    var context: Context = ctx
    private val toggleDriverFake = Observable.just(false)
    private val toggleObserverFake = TestObserver<Boolean>()
    private val toggleConsumerFake = TestConsumer(toggleObserverFake) as Consumer<Boolean>
    private val expectedVersionNumber = BuildConfig.VERSION_NAME

    fun createListOfSettings(): List<SettingCellConfiguration> {

        return listOf(
            ToggleSettingConfiguration(
                title = context.getString(R.string.unlock),
                toggleDriver = toggleDriverFake,
                toggleObserver = toggleConsumerFake
            ),
            TextSettingConfiguration(
                title = context.getString(R.string.auto_lock),
                detailText = context.getString(R.string.auto_lock_option)
            )
        )
    }

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
                title = "Auto lock",
                detailText = "5 minutes"
            ),
            ToggleSettingConfiguration(
                title = context.getString(R.string.autofill),
                subtitle = context.getString(R.string.autofill_summary),
                toggleDriver = toggleDriverFake,
                toggleObserver = toggleConsumerFake
            ),
            ToggleSettingConfiguration(
                title = context.getString(R.string.send_usage_data),
                subtitle = context.getString(R.string.send_usage_data_summary),
                buttonTitle = context.getString(R.string.learn_more),
                toggleDriver = toggleDriverFake,
                toggleObserver = toggleConsumerFake
            ),
            AppVersionSettingConfiguration(
                text = "App Version: $expectedVersionNumber"
            )
        )
        if (isFingerprintAvailable) {
            settings = listOf(
                ToggleSettingConfiguration(
                    title = context.getString(R.string.unlock),
                    toggleDriver = toggleDriverFake,
                    toggleObserver = toggleConsumerFake
                )
            ) + settings
        }
        return settings
    }
}
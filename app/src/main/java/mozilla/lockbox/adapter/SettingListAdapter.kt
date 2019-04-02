/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.view.AppVersionSettingViewHolder
import mozilla.lockbox.view.SettingViewHolder
import mozilla.lockbox.view.TextSettingViewHolder
import mozilla.lockbox.view.ToggleSettingViewHolder

class SettingListAdapter : RecyclerView.Adapter<SettingViewHolder>() {
    private var settingListConfig: List<SettingCellConfiguration> = emptyList()
    private val compositeDisposable = CompositeDisposable()

    companion object {
        const val SETTING_TEXT_TYPE = 0
        const val SETTING_TOGGLE_TYPE = 1
        const val SETTING_APP_VERSION_TYPE = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            SETTING_TEXT_TYPE -> {
                val view = inflater.inflate(R.layout.list_cell_setting_text, parent, false)
                return TextSettingViewHolder(view)
            }
            SETTING_TOGGLE_TYPE -> {
                val view = inflater.inflate(R.layout.list_cell_setting_toggle, parent, false)
                return ToggleSettingViewHolder(view)
            }
            SETTING_APP_VERSION_TYPE -> {
                val view = inflater.inflate(R.layout.list_cell_setting_appversion, parent, false)
                return AppVersionSettingViewHolder(view)
            }
            else -> {
                throw IllegalStateException("Please use a valid defined setting type.")
            }
        }
    }

    override fun getItemCount(): Int {
        return settingListConfig.count()
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.disposable?.let { compositeDisposable.remove(it) }

        val configuration = settingListConfig[position]
        when {
            holder is TextSettingViewHolder && configuration is TextSettingConfiguration -> {
                holder.title = configuration.title
                holder.contentDescription = configuration.contentDescription
                holder.itemView.clicks()
                    .subscribe(configuration.clickListener)
                    .addTo(compositeDisposable)
                configuration.detailTextDriver
                    .subscribe {
                        holder.detailTextRes = it
                    }
                    .addTo(compositeDisposable)
            }
            holder is ToggleSettingViewHolder && configuration is ToggleSettingConfiguration -> {
                holder.title = configuration.title
                holder.subtitle = configuration.subtitle ?: R.string.empty_string
                holder.buttonTitle = configuration.buttonTitle ?: R.string.empty_string
                holder.contentDescription = configuration.contentDescription
                configuration.buttonObserver?.let {
                    holder.buttonClicks
                        .subscribe(it)
                        .addTo(compositeDisposable)
                }
                configuration.toggleDriver
                    .subscribe {
                        holder.toggle.isChecked = it
                    }
                    .addTo(compositeDisposable)
                holder.toggleValueChanges
                    .skip(1)
                    .subscribe(configuration.toggleObserver)
                    .addTo(compositeDisposable)
            }
            holder is AppVersionSettingViewHolder && configuration is AppVersionSettingConfiguration -> {
                holder.setTitle(configuration.title, configuration.appVersion, configuration.buildNumber)
                holder.contentDescription = configuration.contentDescription
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val configurationType = settingListConfig[position]
        return when (configurationType) {
            is TextSettingConfiguration -> SETTING_TEXT_TYPE
            is ToggleSettingConfiguration -> SETTING_TOGGLE_TYPE
            is AppVersionSettingConfiguration -> SETTING_APP_VERSION_TYPE
            else -> throw IllegalStateException("Please use a valid defined setting type.")
        }
    }

    fun setItems(settingConfig: List<SettingCellConfiguration>) {
        settingListConfig = settingConfig
        notifyDataSetChanged()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        compositeDisposable.clear()
    }
}

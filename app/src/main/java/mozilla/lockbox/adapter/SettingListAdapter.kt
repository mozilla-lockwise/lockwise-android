/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.view.AppVersionSettingViewHolder
import mozilla.lockbox.view.SettingViewHolder
import mozilla.lockbox.view.TextSettingViewHolder
import mozilla.lockbox.view.ToggleSettingViewHolder

class SettingListAdapter : RecyclerView.Adapter<SettingViewHolder>() {
    private var settingListConfig: List<SettingCellConfiguration> = emptyList()
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
            else -> {
                val view = inflater.inflate(R.layout.list_cell_setting_appversion, parent, false)
                return AppVersionSettingViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int {
        return settingListConfig.count()
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.compositeDisposable.clear()

        val configuration = settingListConfig[position]
        when {
            holder is TextSettingViewHolder && configuration is TextSettingConfiguration -> {
                holder.title = configuration.title
                holder.detailText = configuration.detailText
            }
            holder is ToggleSettingViewHolder && configuration is ToggleSettingConfiguration -> {
                holder.title = configuration.title
                holder.subtitle = configuration.subtitle
                holder.buttonTitle = configuration.buttonTitle
                configuration.toggle.subscribe(holder.toggle).addTo(holder.compositeDisposable)
            }
            holder is AppVersionSettingViewHolder && configuration is AppVersionSettingConfiguration -> {
                holder.text = configuration.text
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val configurationType = settingListConfig[position]
        when (configurationType) {
            is TextSettingConfiguration -> {
                return SETTING_TEXT_TYPE
            }
            is ToggleSettingConfiguration -> {
                return SETTING_TOGGLE_TYPE
            }
            else -> {
                return SETTING_APP_VERSION_TYPE
            }
        }
    }

    fun setItems(settingConfig: List<SettingCellConfiguration>) {
        settingListConfig = settingConfig
        notifyDataSetChanged()
    }
}

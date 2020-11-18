/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.magiepooh.recycleritemdecoration.ItemDecorations
import kotlinx.android.synthetic.main.fragment_setting.view.*
import kotlinx.android.synthetic.main.list_cell_setting_toggle.view.*
import mozilla.lockbox.R
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.adapter.SettingCellConfiguration
import mozilla.lockbox.adapter.SettingListAdapter
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_TEXT_TYPE
import mozilla.lockbox.adapter.SettingListAdapter.Companion.SETTING_TOGGLE_TYPE
import mozilla.lockbox.presenter.SettingPresenter
import mozilla.lockbox.presenter.SettingView

class SettingFragment : BackableFragment(), SettingView {

    private val adapter = SettingListAdapter()
    @Suppress("UNCHECKED_CAST")
    private val sectionedAdapter = SectionedAdapter(
        R.layout.list_cell_setting_header,
        R.id.headerTitle,
        adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = SettingPresenter(this)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        view.settingList.adapter = sectionedAdapter
        val layoutManager = LinearLayoutManager(context)
        view.settingList.layoutManager = layoutManager

        // Add one to account for viewType configuration in the SectionedAdapter
        val decoration = ItemDecorations.vertical(context)
            .type(SETTING_TEXT_TYPE + 1, R.drawable.divider)
            .type(SETTING_TOGGLE_TYPE + 1, R.drawable.divider)
            .create()

        view.settingList.addItemDecoration(decoration)

        return view
    }

    override fun updateSettingList(
        settings: List<SettingCellConfiguration>,
        sections: List<SectionedAdapter.Section>
    ) {
        adapter.setItems(settings)
        sectionedAdapter.setSections(sections)
    }

    override fun onDestroyView() {
        adapter.onDetachedFromRecyclerView(requireView().settingList)
        super.onDestroyView()
    }
}

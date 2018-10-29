/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_setting.view.*
import mozilla.lockbox.R
import mozilla.lockbox.adapter.AccountAdapter
import mozilla.lockbox.adapter.SectionedAdapter
import mozilla.lockbox.presenter.AccountPresenter
import mozilla.lockbox.presenter.AccountView

class AccountFragment : BackableFragment(), AccountView {
    private val accountAdapter = AccountAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = AccountPresenter(this)
        val view = inflater.inflate(
            R.layout.fragment_account,
            container,
            false)

        view.settingList.adapter = accountAdapter
        val layoutManager = LinearLayoutManager(context)
        view.settingList.layoutManager = layoutManager

    }
}
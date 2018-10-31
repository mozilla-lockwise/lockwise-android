/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mozilla.lockbox.R
import mozilla.lockbox.adapter.AccountAdapter
import mozilla.lockbox.adapter.AccountConfiguration
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

        return inflater.inflate(R.layout.fragment_account, container, false)
//        view.adapter = accountAdapter
//        val layoutManager = LinearLayoutManager(context)
//        view.layoutManager = layoutManager
//
//        return view
    }

    override fun update(account: AccountConfiguration) {
        accountAdapter.setAccountItems(account)
    }
}
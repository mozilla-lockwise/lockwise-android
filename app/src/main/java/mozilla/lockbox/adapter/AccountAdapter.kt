/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_account.*
import mozilla.lockbox.R
import mozilla.lockbox.view.AccountViewHolder

class AccountAdapter: RecyclerView.Adapter<AccountViewHolder>(){

    private lateinit var accountList: AccountConfiguration

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.fragment_account, parent, false)
        return AccountViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 0
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val configuration = accountList
        holder.email = configuration.email
        holder.disconnectSummary = configuration.summary
        holder.buttonTitle = configuration.buttonTitle
        holder.buttonSummary = configuration.buttonSummary
    }

    fun setAccountItems(fxaAccount: AccountConfiguration){
        accountList = fxaAccount
        notifyDataSetChanged()
    }
}
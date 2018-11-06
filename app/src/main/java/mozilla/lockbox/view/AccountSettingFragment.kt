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
import kotlinx.android.synthetic.main.include_backable.*
import mozilla.lockbox.R
import mozilla.lockbox.R.string.*
import mozilla.lockbox.presenter.AccountSettingPresenter
import mozilla.lockbox.presenter.AccountSettingView

class AccountSettingFragment : BackableFragment(), AccountSettingView {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View?
    {
        presenter = AccountSettingPresenter(this)
        return inflater.inflate(R.layout.fragment_account_setting, container, false)
    }
}
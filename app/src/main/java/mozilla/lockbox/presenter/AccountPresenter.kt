/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.v7.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import mozilla.lockbox.R.string.email_example
import mozilla.lockbox.R.string.button_summary
import mozilla.lockbox.R.string.button_title
import mozilla.lockbox.R.string.disconnect_account_summary
import mozilla.lockbox.adapter.AccountConfiguration
import mozilla.lockbox.flux.Presenter

interface AccountView {
    fun update(account: AccountConfiguration)
}
class AccountPresenter(val view: AccountView) : Presenter() {
    override fun onViewReady() {
        val fxaAccount = AccountConfiguration(
            email = email_example.toString(),
            summary = disconnect_account_summary.toString(),
            buttonTitle = button_title.toString(),
            buttonSummary = button_summary.toString()
        )

        view.update(fxaAccount)
    }
}
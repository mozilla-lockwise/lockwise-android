/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import junit.framework.Assert.assertEquals
import mozilla.lockbox.R.string.button_summary
import mozilla.lockbox.R.string.button_title
import mozilla.lockbox.R.string.disconnect_account_summary
import mozilla.lockbox.R.string.email_example
import mozilla.lockbox.adapter.AccountConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AccountPresenterTests {

    private lateinit var context: Context
    private val accountView = AccountViewFake()
    private val subject = AccountPresenter(accountView)

    class AccountViewFake : AccountView {
        var fxaAccount: AccountConfiguration? = null
        override fun update(account: AccountConfiguration) {
            fxaAccount = account
        }
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
    }

    @Test
    fun onViewReadyTest() {

        val expectedAccount = AccountConfiguration(
            email = email_example.toString(),
            summary = disconnect_account_summary.toString(),
            buttonTitle = button_title.toString(),
            buttonSummary = button_summary.toString()
        )
        subject.onViewReady()

        assertEquals(accountView.fxaAccount!!.email, expectedAccount.email)
        assertEquals(accountView.fxaAccount!!.summary, expectedAccount.summary)
        assertEquals(accountView.fxaAccount!!.buttonTitle, expectedAccount.buttonTitle)
        assertEquals(accountView.fxaAccount!!.buttonSummary, expectedAccount.buttonSummary)
    }
}
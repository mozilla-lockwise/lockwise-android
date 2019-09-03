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
import androidx.transition.Visibility
import io.reactivex.Observable
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.fragment_welcome.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.presenter.WelcomePresenter
import mozilla.lockbox.presenter.WelcomeView
import mozilla.lockbox.store.AccountStore

class WelcomeFragment : Fragment(), WelcomeView {
    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = WelcomePresenter(this)
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        val appLabel = getString(R.string.app_label)
        view.textViewInstructions.text = getString(R.string.welcome_instructions, appLabel)
        view.lockwiseIcon.contentDescription = getString(R.string.app_logo, appLabel)

        val existingAccount = AccountStore.shared.shareableAccount()
        if (existingAccount != null) {
            view.buttonGetStarted.text = getString(R.string.welcome_start_automatic_btn, existingAccount.email)
            view.buttonGetStartedManually.text = getString(R.string.welcome_start_force_manual_btn)
        } else {
            view.buttonGetStartedManually.visibility = View.GONE
        }
        return view
    }

    override val getStartedAutomaticallyClicks: Observable<Unit>
        get() = view!!.buttonGetStarted.clicks()

    override val getStartedManuallyClicks: Observable<Unit>
        get() = view!!.buttonGetStartedManually.clicks()

    override val learnMoreClicks: Observable<Unit>
        get() = view!!.textViewLearnMore.clicks()
}
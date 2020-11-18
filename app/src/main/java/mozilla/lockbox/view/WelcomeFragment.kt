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
import io.reactivex.Observable
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.fragment_welcome.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.presenter.WelcomePresenter
import mozilla.lockbox.presenter.WelcomeView

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

        return view
    }

    override fun showExistingAccount(email: String) {
        view?.apply {
            buttonGetStarted.text = getString(R.string.welcome_start_automatic_btn, email)
            buttonGetStarted.visibility = View.VISIBLE

            buttonGetStartedManually.text = getString(R.string.welcome_start_force_manual_btn)
            buttonGetStartedManually.visibility = View.VISIBLE
        }
    }

    override fun hideExistingAccount() {
        view?.apply {
            buttonGetStarted.visibility = View.GONE

            buttonGetStartedManually.text = getString(R.string.welcome_start_btn)
            buttonGetStartedManually.visibility = View.VISIBLE
        }
    }

    override val getStartedAutomaticallyClicks: Observable<Unit>
        get() = requireView().buttonGetStarted.clicks()

    override val getStartedManuallyClicks: Observable<Unit>
        get() = requireView().buttonGetStartedManually.clicks()

    override val learnMoreClicks: Observable<Unit>
        get() = requireView().textViewLearnMore.clicks()
}

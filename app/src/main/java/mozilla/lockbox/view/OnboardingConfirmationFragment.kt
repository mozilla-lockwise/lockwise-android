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
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_onboarding_confirmation.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.OnboardingConfirmationPresenter
import mozilla.lockbox.presenter.OnboardingConfirmationView

class OnboardingConfirmationFragment : Fragment(), OnboardingConfirmationView {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = OnboardingConfirmationPresenter(this)

        return inflater.inflate(R.layout.fragment_onboarding_confirmation, container, false)
    }

    override val finishClicks: Observable<Unit>
        get() = requireView().finishButton.clicks()
}
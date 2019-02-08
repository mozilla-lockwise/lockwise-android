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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = OnboardingConfirmationPresenter(this)

        return inflater.inflate(R.layout.fragment_onboarding_confirmation, container, false)
    }

    override val finishClicks: Observable<Unit>
        get() = view!!.finish_button.clicks()
}
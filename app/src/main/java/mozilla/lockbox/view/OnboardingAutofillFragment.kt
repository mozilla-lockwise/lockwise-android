package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.onboarding_autofill.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.presenter.OnboardingAutofillPresenter
import mozilla.lockbox.presenter.OnboardingAutofillView

@ExperimentalCoroutinesApi
class OnboardingAutofillFragment : Fragment(), OnboardingAutofillView {

    override val onDismiss: Observable<Unit>
        get() = view!!.skipOnboarding.clicks()

    override val onEnable: Observable<Unit>
        get() = view!!.goToSettings.clicks()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = OnboardingAutofillPresenter(this)
        return inflater.inflate(R.layout.onboarding_autofill, container, false)
    }
}
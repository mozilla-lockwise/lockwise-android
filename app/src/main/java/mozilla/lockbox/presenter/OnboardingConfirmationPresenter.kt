package mozilla.lockbox.presenter

import io.reactivex.Observable
import mozilla.lockbox.flux.Presenter

interface OnboardingConfirmationView {
    val finishClicks: Observable<Unit>
}

class OnboardingConfirmationPresenter(
    view: OnboardingConfirmationView
) : Presenter() {
    override fun onViewReady() {
    }
}
package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface OnboardingConfirmationView {
    val finishClicks: Observable<Unit>
}

class OnboardingConfirmationPresenter(
    val view: OnboardingConfirmationView,
    val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {
    override fun onViewReady() {
        view.finishClicks
            .map { OnboardingStatusAction(false) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }
}
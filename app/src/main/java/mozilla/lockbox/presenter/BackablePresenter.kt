package mozilla.lockbox.presenter

import androidx.annotation.CallSuper
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface BackableView {
    val backButtonClicks: Observable<Unit>
}

class BackablePresenter(
    val view: BackableView,
    val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {
    @CallSuper
    override fun onViewReady() {
        view.backButtonClicks
            .subscribe {
                dispatcher.dispatch(RouteAction.InternalBack)
            }
            .addTo(compositeDisposable)
    }
}
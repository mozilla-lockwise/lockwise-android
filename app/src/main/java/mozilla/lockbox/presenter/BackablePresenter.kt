package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface BackableViewProtocol {
    val backButtonTaps: Observable<Unit>
}

class BackablePresenter(val view: BackableViewProtocol,
                        val dispatcher: Dispatcher = Dispatcher.shared) : Presenter() {
    override fun onViewReady() {
        view.backButtonTaps
                .subscribe { dispatcher.dispatch(RouteAction.BACK) }
                .addTo(compositeDisposable)
    }
}
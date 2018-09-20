package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface WelcomeViewProtocol {
    val getStartedClicks: Observable<Unit>
}

class WelcomePresenter(private val protocol: WelcomeViewProtocol, private val dispatcher: Dispatcher = Dispatcher.shared) : Presenter() {
    override fun onViewReady() {
        protocol.getStartedClicks.subscribe { _ ->
            dispatcher.dispatch(RouteAction.LOGIN)
        }.addTo(compositeDisposable)
    }
}

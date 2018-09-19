package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

interface WelcomeViewProtocol {
    val getStartedClicks: Observable<Unit>
}

class WelcomePresenter(private val protocol: WelcomeViewProtocol, val dispatcher: Dispatcher = Dispatcher.shared) {

    private val compositeDisposable = CompositeDisposable()

    fun onViewReady() {
        protocol.getStartedClicks.subscribe { _ ->
            dispatcher.dispatch(RouteAction.LOGIN)
        }.addTo(compositeDisposable)
    }

    fun onDestroy() {
        compositeDisposable.clear()
    }
}
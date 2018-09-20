package mozilla.lockbox.store
import io.reactivex.Observable
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

class RouteStore(dispatcher: Dispatcher = Dispatcher.shared) {
    companion object {
        val shared = RouteStore()
    }

    val routes: Observable<RouteAction> = dispatcher.register
            .filterByType(RouteAction::class.java)
}

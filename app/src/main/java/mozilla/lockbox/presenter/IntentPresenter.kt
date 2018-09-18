package mozilla.lockbox.presenter

import android.util.Log
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.RouteStore

class IntentPresenter(routeStore: RouteStore = RouteStore.shared) {
    companion object {
        val shared = IntentPresenter()
    }

    init {
        routeStore.routes.subscribe { a -> route(a) }
    }

    fun route(action: RouteAction) {
        Log.i("dfsadfs", "action: ${action.name}")
    }
}
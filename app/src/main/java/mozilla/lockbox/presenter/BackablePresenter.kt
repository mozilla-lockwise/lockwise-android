package mozilla.lockbox.presenter

import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface BackableView {
    // No methods yet.
}

class BackablePresenter(
    val view: BackableView,
    val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {
    // No methods yet.
}
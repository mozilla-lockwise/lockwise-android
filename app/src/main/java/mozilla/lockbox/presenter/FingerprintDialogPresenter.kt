/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.AuthenticationAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore

interface FingerprintDialogView {
    fun onSucceeded()
    fun onFailed()
    fun onError(error: String?)
    val onAuthenticated: Observable<Unit>
    val fallbackToPin: Observable<Unit>
}

class FingerprintDialogPresenter(
    private val view: FingerprintDialogView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) :
    Presenter() {

    override fun onViewReady() {
        fingerprintStore.authState
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::updateState).addTo(compositeDisposable)
        view.onAuthenticated
            .subscribe { dispatcher.dispatch(RouteAction.ItemList) }
            .addTo(compositeDisposable)
//        view.fallbackToPin
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { dispatcher.dispatch(RouteAction.ItemList) }
//            .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        dispatcher.dispatch(AuthenticationAction.StartListening)
    }

    override fun onPause() {
        super.onPause()
        dispatcher.dispatch(AuthenticationAction.StopListening)
    }

    private fun updateState(state: FingerprintStore.AuthenticationState) {
        when (state) {
            is FingerprintStore.AuthenticationState.Succeeded -> view.onSucceeded()
            is FingerprintStore.AuthenticationState.Failed -> view.onFailed()
            is FingerprintStore.AuthenticationState.Error -> view.onError(state.error)
        }
    }
}
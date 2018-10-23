/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState
import mozilla.lockbox.view.FingerprintAuthDialogFragment.AuthCallback

interface FingerprintDialogView {
    fun onSucceeded()
    fun onFailed()
    fun onError(error: String?)
    val authCallback: Observable<AuthCallback>
    val cancelTapped: Observable<Unit>
    fun onCancel()
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
            .subscribe(this::updateState)
            .addTo(compositeDisposable)

        view.authCallback
            .subscribe {
                when (it) {
                    is AuthCallback.OnAuth -> dispatcher.dispatch(RouteAction.ItemList)
                    is AuthCallback.OnError -> dispatcher.dispatch(RouteAction.LockScreen)
                }
            }
            .addTo(compositeDisposable)

        view.cancelTapped
            .subscribe { view.onCancel() }
            .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        dispatcher.dispatch(FingerprintSensorAction.Start)
    }

    override fun onPause() {
        super.onPause()
        dispatcher.dispatch(FingerprintSensorAction.Stop)
    }

    private fun updateState(state: AuthenticationState) {
        when (state) {
            is AuthenticationState.Succeeded -> view.onSucceeded()
            is AuthenticationState.Failed -> view.onFailed()
            is AuthenticationState.Error -> view.onError(state.error)
        }
    }
}
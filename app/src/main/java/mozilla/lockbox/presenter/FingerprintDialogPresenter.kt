/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintAuthAction.OnAuthentication
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState
import mozilla.lockbox.view.FingerprintAuthDialogFragment.AuthCallback

interface FingerprintDialogView {
    fun onSucceeded()
    fun onFailed(error: String?)
    fun onError(error: String?)
    val authCallback: Observable<AuthCallback>
    val onDismiss: Observable<Unit>
}

class FingerprintDialogPresenter(
    private val view: FingerprintDialogView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {
        fingerprintStore.authState
            .subscribe(this::updateState)
            .addTo(compositeDisposable)

        view.authCallback
            .subscribe { dispatcher.dispatch(OnAuthentication(it)) }
            .addTo(compositeDisposable)

        view.onDismiss
            .subscribe { dispatcher.dispatch(FingerprintAuthAction.OnCancel) }
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
            is AuthenticationState.Failed -> view.onFailed(state.error)
            is AuthenticationState.Error -> view.onError(state.error)
        }
    }
}
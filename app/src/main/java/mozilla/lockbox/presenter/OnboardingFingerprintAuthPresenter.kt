/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.OnboardingAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState as AuthenticationState
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment.AuthCallback as AuthCallback

interface OnboardingFingerprintView {
    fun onSucceeded()
    fun onFailed(error: String?)
    fun onError(error: String?)
    val onDismiss: Observable<Unit>
    val authCallback: Observable<AuthCallback>
}

@ExperimentalCoroutinesApi
class OnboardingFingerprintAuthPresenter(
    private val view: OnboardingFingerprintView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {
        if (fingerprintStore.isFingerprintAuthAvailable) {

            routeStore.onboarding
                .subscribe(this::startOnboarding)
                .addTo(compositeDisposable)

            fingerprintStore.authState
                .subscribe(this::updateState)
                .addTo(compositeDisposable)

            view.onDismiss
                .subscribe {
                    dispatcher.dispatch(OnboardingAction.OnDismiss)
                }.addTo(compositeDisposable)

            view.authCallback
                .subscribe { dispatcher.dispatch(OnboardingAction.OnAuthentication(it)) }
                .addTo(compositeDisposable)
        } else {
            dispatcher.dispatch(RouteAction.SkipOnboarding)
        }
    }

    override fun onResume() {
        super.onResume()
        dispatcher.dispatch(FingerprintSensorAction.Start)
    }

    override fun onPause() {
        super.onPause()
        dispatcher.dispatch(FingerprintSensorAction.Stop)
    }

    private fun updateState(state: FingerprintStore.AuthenticationState) {
        when (state) {
            is AuthenticationState.Succeeded -> view.onSucceeded()
            is AuthenticationState.Failed -> view.onFailed(state.error)
            is AuthenticationState.Error -> view.onError(state.error)
        }
    }

    private fun startOnboarding(state: Boolean) {
        when (state) {
            state -> onResume()
            else -> dispatcher.dispatch(RouteAction.SkipOnboarding)
        }
    }
}

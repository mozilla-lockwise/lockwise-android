/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.OnboardingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment

interface OnboardingFingerprintView {
    fun onSucceeded()
    fun onFailed(error: String?)
    fun onError(error: String?)
    val startOnboarding: Observable<Unit>
    val onDismiss: Observable<Unit>
    val authCallback: Observable<OnboardingFingerprintAuthFragment.AuthCallback>
}

class OnboardingFingerprintAuthPresenter(
    private val view: OnboardingFingerprintView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val routeStore: RouteStore = RouteStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {

        // check if fingerprint is available (do this before we get to the presenter? no)
        if (fingerprintStore.isFingerprintAuthAvailable) {
            routeStore.onboarding
                .subscribe(this::triggerOnboarding)
                .addTo(compositeDisposable)

            view.onDismiss
                .subscribe {
                    dispatcher.dispatch(OnboardingAction.OnDismiss)
                }.addTo(compositeDisposable)

            view.startOnboarding
                .subscribe {
                    dispatcher.dispatch(OnboardingAction.ShowOnboarding)
                }.addTo(compositeDisposable)

            view.authCallback
                .subscribe { dispatcher.dispatch(OnboardingAction.OnAuthentication(it)) }
                .addTo(compositeDisposable)
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

    private fun triggerOnboarding(state: Boolean) {
        val action = when (state) {
            true -> OnboardingAction.ShowOnboarding
            false -> OnboardingAction.OnDismiss
        }
        dispatcher.dispatch(action)
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.os.Build
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState
import mozilla.lockbox.store.SettingStore

interface FingerprintOnboardingView {
    fun onSucceeded()
    fun onFailed(error: String?)
    fun onError(error: String?)
    val onSkipClick: Observable<Unit>
    val authCallback: Observable<FingerprintAuthAction>
}

@ExperimentalCoroutinesApi
class FingerprintOnboardingPresenter(
    private val view: FingerprintOnboardingView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {
        fingerprintStore.authState
            .subscribe(this::updateState)
            .addTo(compositeDisposable)

        view.authCallback
            .subscribe {
                dispatcher.dispatch(it)
                val unlock = when (it) {
                    is FingerprintAuthAction.OnSuccess -> true
                    else -> false
                }
                dispatcher.dispatch(SettingAction.UnlockWithFingerprint(unlock))
                triggerNextOnboarding()
            }
            .addTo(compositeDisposable)

        view.onSkipClick.subscribe {
            dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
            triggerNextOnboarding()
        }?.addTo(compositeDisposable)
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

    private fun triggerNextOnboarding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (SettingStore.shared.autofillAvailable) {
                dispatcher.dispatch(RouteAction.Onboarding.Autofill)
            }
        } else {
            log.info("Autofill not available.")
            dispatcher.dispatch(RouteAction.Onboarding.Confirmation)
        }
    }
}

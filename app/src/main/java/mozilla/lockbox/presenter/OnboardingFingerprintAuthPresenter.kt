/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState as AuthenticationState

interface OnboardingFingerprintView {
    fun onSucceeded()
    fun onFailed(error: String?)
    fun onError(error: String?)
    val onDismiss: Observable<Unit>
    val authCallback: Observable<FingerprintAuthCallback>
}

@ExperimentalCoroutinesApi
class OnboardingFingerprintAuthPresenter(
    private val view: OnboardingFingerprintView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {
        fingerprintStore.authState
            .subscribe(this::updateState)
            .addTo(compositeDisposable)

        view.authCallback
            .subscribe {
                dispatcher.dispatch(FingerprintAuthAction.OnAuthentication(it))
                val unlock = when (it) {
                    is FingerprintAuthCallback.OnAuth -> true
                    else -> false
                }
                dispatcher.dispatch(SettingAction.UnlockWithFingerprint(unlock))
                dispatcher.dispatch(OnboardingStatusAction(false))
            }
            .addTo(compositeDisposable)

        view.onDismiss.subscribe {
            dispatcher.dispatch(OnboardingStatusAction(false))
            dispatcher.dispatch(SettingAction.UnlockWithFingerprint(false))
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
}

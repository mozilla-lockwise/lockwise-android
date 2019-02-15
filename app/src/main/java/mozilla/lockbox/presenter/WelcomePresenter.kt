/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore

interface WelcomeView {
    val getStartedClicks: Observable<Unit>
    val learnMoreClicks: Observable<Unit>
}

class WelcomePresenter(
    private val view: WelcomeView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {
    override fun onViewReady() {
        val isDeviceSecure: Boolean = fingerprintStore.isKeyguardDeviceSecure

        view.getStartedClicks
            .map {
                if (isDeviceSecure)
                    RouteAction.Login
                else {
                    RouteAction.DialogFragment.OnboardingSecurityDialog(
                        R.string.secure_your_device,
                        R.string.device_security_description
                    )
                }
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.learnMoreClicks
            .subscribe { dispatcher.dispatch(RouteAction.AppWebPage.FaqWelcome) }
            .addTo(compositeDisposable)
    }
}
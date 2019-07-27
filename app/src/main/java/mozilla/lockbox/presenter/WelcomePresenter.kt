/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.service.fxa.sharing.AccountSharing
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.FingerprintStore

interface WelcomeView {
    val getStartedAutomaticallyClicks: Observable<Unit>
    val getStartedManuallyClicks: Observable<Unit>
    val learnMoreClicks: Observable<Unit>
}

class WelcomePresenter(
    private val view: WelcomeView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {
    @ExperimentalCoroutinesApi
    override fun onViewReady() {
        view.getStartedAutomaticallyClicks
            .map {
                val availableAccount = AccountStore.shared.shareableAccount()
                if (availableAccount != null) {
                    if (fingerprintStore.isKeyguardDeviceSecure) {
                        AccountAction.AutomaticLogin(availableAccount)
                    } else {
                        DialogAction.OnboardingSecurityDialogAutomatic(availableAccount)
                    }
                } else if (fingerprintStore.isKeyguardDeviceSecure) {
                    RouteAction.Login
                } else {
                    DialogAction.OnboardingSecurityDialogManual
                }
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.getStartedManuallyClicks
            .map {
                if (fingerprintStore.isKeyguardDeviceSecure) {
                    RouteAction.Login
                } else {
                    DialogAction.OnboardingSecurityDialogManual
                }
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.learnMoreClicks
            .subscribe {
                dispatcher.dispatch(AppWebPageAction.FaqWelcome)
            }
            .addTo(compositeDisposable)
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.service.fxa.sharing.ShareableAccount
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.support.FeatureFlags

interface WelcomeView {
    val getStartedAutomaticallyClicks: Observable<Unit>
    val getStartedManuallyClicks: Observable<Unit>
    val learnMoreClicks: Observable<Unit>
    fun showExistingAccount(email: String)
    fun hideExistingAccount()
}

@ExperimentalCoroutinesApi
class WelcomePresenter(
    private val view: WelcomeView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val accountStore: AccountStore = AccountStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {

    override fun onViewReady() {
        if (FeatureFlags.FXA_LOGIN_WITTH_AUTHPROVIDER) {
            accountStore.shareableAccount()?.let { account ->
                view.showExistingAccount(account.email)
                view.getStartedAutomaticallyClicks
                    .map {
                        routeToExistingAccount(account)
                    }
                    .subscribe(dispatcher::dispatch)
                    .addTo(compositeDisposable)
            } ?: view.hideExistingAccount()
        } else {
            view.hideExistingAccount()
        }

        view.getStartedManuallyClicks
            .map {
                routeToLoginManually()
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.learnMoreClicks
            .subscribe {
                dispatcher.dispatch(AppWebPageAction.FaqWelcome)
            }
            .addTo(compositeDisposable)
    }

    private fun routeToExistingAccount(account: ShareableAccount) =
        if (fingerprintStore.isKeyguardDeviceSecure) {
            AccountAction.AutomaticLogin(account)
        } else {
            DialogAction.OnboardingSecurityDialogAutomatic(account)
        }

    private fun routeToLoginManually() =
        if (fingerprintStore.isKeyguardDeviceSecure) {
            RouteAction.Login
        } else {
            DialogAction.OnboardingSecurityDialogManual
        }
}

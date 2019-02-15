/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface OnboardingSecurityDialogView {
    val onSkipClick: Observable<Unit>
    val onSetUpNowClick: Observable<Unit>
}

class OnboardingSecurityDialogPresenter(
    private val view: OnboardingSecurityDialogView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {
    override fun onViewReady() {
        view.onSkipClick
            .subscribe {
                dispatcher.dispatch(RouteAction.Login)
            }
            .addTo(compositeDisposable)

        view.onSetUpNowClick
            .subscribe {
                dispatcher.dispatch(RouteAction.SystemSetting(SettingIntent.Security))
                dispatcher.dispatch(RouteAction.Login)
            }
            .addTo(compositeDisposable)
    }
}
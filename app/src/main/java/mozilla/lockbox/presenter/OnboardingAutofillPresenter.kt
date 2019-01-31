/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter

interface OnboardingAutofillView {
    val onDismiss: Observable<Unit>
    val onEnable: Observable<Unit>
}

@ExperimentalCoroutinesApi
class OnboardingAutofillPresenter(
    private val view: OnboardingAutofillView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        view.onDismiss.subscribe {
            dispatcher.dispatch(RouteAction.Onboarding.SkipOnboarding)
        }?.addTo(compositeDisposable)

        view.onEnable.subscribe {
            dispatcher.dispatch(RouteAction.SettingList)
        }?.addTo(compositeDisposable)
    }
}

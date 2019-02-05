/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.asOptional

@ExperimentalCoroutinesApi
open class OnboardingStore(
    dispatcher: Dispatcher = Dispatcher.shared,
    dataStore: DataStore = DataStore.shared,
    fingerprintStore: FingerprintStore = FingerprintStore.shared,
    autofillStore: AutofillStore = AutofillStore.shared,
    routeStore: RouteStore = RouteStore.shared
) {
    internal val compositeDisposable = CompositeDisposable()

    private val onboardingState: ReplaySubject<Boolean> = ReplaySubject.createWithSize(1)
    open val onboarding: Observable<Boolean> = onboardingState

    open var triggerFingerprintAuthOnboarding: Boolean = false
    open var triggerAutofillOnboarding: Boolean = false

    companion object {
        val shared = OnboardingStore()
    }

    init {
        dispatcher.register
            .filterByType(RouteAction.Onboarding.ChooseRoute::class.java)
            .subscribe{
            }
            .addTo(compositeDisposable)

        this.onboarding
            .subscribe { ob ->
                triggerFingerprintAuthOnboarding = ob && fingerprintStore.isFingerprintAuthAvailable
                triggerAutofillOnboarding = ob && autofillStore.isAutofillEnabledAndSupported
            }
            .addTo(compositeDisposable)

        dispatcher.register
            .filterByType(RouteAction.Onboarding.SkipOnboarding::class.java)
            .subscribe {
                onboardingState.onNext(false)
                // do we care if we choose the route here or not?
                routeStore.chooseRoute().asOptional()
            }.addTo(compositeDisposable)

        dataStore.state
            .subscribe { state ->
                when (state) {
                    is DataStore.State.Unprepared -> onboardingState.onNext(true)
                    else -> onboardingState.onNext(false)
                }
            }
            .addTo(compositeDisposable)
    }
}
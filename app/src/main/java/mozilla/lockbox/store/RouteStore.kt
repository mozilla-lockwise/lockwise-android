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
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.OnboardingAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional

@ExperimentalCoroutinesApi
class RouteStore(
    dispatcher: Dispatcher = Dispatcher.shared,
    dataStore: DataStore = DataStore.shared
) {
    internal val compositeDisposable = CompositeDisposable()

    private val onboardingState: ReplaySubject<Boolean> = ReplaySubject.createWithSize(1)
    val onboarding: Observable<Boolean> = onboardingState
    private var triggerOnboarding: Boolean? = null

    companion object {
        val shared = RouteStore()
    }

    val routes: Observable<RouteAction> = ReplaySubject.createWithSize(1)

    init {
        dispatcher.register
            .filterByType(RouteAction::class.java)
            .subscribe(routes as Subject)

        this.onboarding
            .subscribe { ob ->
                triggerOnboarding = ob
            }
            .addTo(compositeDisposable)

        dispatcher.register
            .filterByType(OnboardingAction.OnDismiss::class.java)
            .subscribe {
                onboardingState.onNext(false)
                chooseRoute()
            }.addTo(compositeDisposable)

        dataStore.state
            .map(this::dataStoreToRouteActions)
            .filterNotNull()
            .subscribe(routes)

        dataStore.state
            .subscribe { state ->
                when (state) {
                    is DataStore.State.Unprepared -> onboardingState.onNext(true)
                    else -> onboardingState.onNext(false)
                }
            }
            .addTo(compositeDisposable)
    }

    private fun dataStoreToRouteActions(storageState: DataStore.State): Optional<RouteAction> {
        return when (storageState) {
            is DataStore.State.Unlocked -> chooseRoute()
            is DataStore.State.Locked -> return RouteAction.LockScreen.asOptional()
            is DataStore.State.Unprepared -> return RouteAction.Welcome.asOptional()
            else -> null!!
        }
    }

    private fun chooseRoute(): Optional<RouteAction> {
        return when (triggerOnboarding) {
            true -> RouteAction.Onboarding.asOptional()
            else -> RouteAction.ItemList.asOptional()
        }
    }
}
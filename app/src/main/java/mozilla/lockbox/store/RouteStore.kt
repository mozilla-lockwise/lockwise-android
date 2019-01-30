/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    companion object {
        val shared = RouteStore()
    }

    val routes: Observable<RouteAction> = ReplaySubject.createWithSize(1)

    init {
        dispatcher.register
            .filterByType(RouteAction::class.java)
            .subscribe(routes as Subject)

        dataStore.state
            .map(this::dataStoreToRouteActions)
            .filterNotNull()
            .subscribe(routes)

        // do we want to onboard if they disconnect their account and
        // then re-login?
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
            is DataStore.State.Unlocked -> chooseRoute()//RouteAction.ItemList
            is DataStore.State.Locked -> return RouteAction.LockScreen.asOptional()
            is DataStore.State.Unprepared -> return RouteAction.Welcome.asOptional()
            // bad. figure out how to make this work again
            else -> null!!
        }
    }

    private var triggerOnboarding: Boolean? = null
    private val triggerOnboardingSubject: Consumer<Boolean>
        get() = Consumer { onboarding ->
            triggerOnboarding = onboarding
        }

    // how do I ensure that ItemList will be routed to after Onboarding completes?
    private fun chooseRoute(): Optional<RouteAction>{
        return when (triggerOnboarding) {
            true -> RouteAction.Onboarding.asOptional()
            else -> RouteAction.ItemList.asOptional()
        }
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
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
    }

    private fun dataStoreToRouteActions(storageState: DataStore.State): Optional<RouteAction> {
        return when (storageState) {
            is DataStore.State.Unlocked -> chooseRoute()
            is DataStore.State.Locked -> RouteAction.LockScreen
            is DataStore.State.Unprepared -> RouteAction.Welcome
            else -> null
        }.asOptional()
    }

    // this can be private if we don't need to call it in the onboarding store
    fun chooseRoute(): RouteAction {
        return when {
            OnboardingStore.shared.triggerFingerprintAuthOnboarding ->
                RouteAction.Onboarding.FingerprintAuth
            OnboardingStore.shared.triggerAutofillOnboarding ->
                RouteAction.Onboarding.Autofill
            else ->
                RouteAction.ItemList
        }
    }
}
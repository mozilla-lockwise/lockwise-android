/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

class OnboardingStoreTest {

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    @Mock
    val dataStore = Mockito.mock(DataStore::class.java)
    @Mock
    val routeStore = Mockito.mock(RouteStore::class.java)

    @Mock
    val fingerprintStore = Mockito.mock(FingerprintStore::class.java)
    var isFingerprintAuthAvailableStub: Boolean = false

    @Mock
    val autofillStore = Mockito.mock(AutofillStore::class.java)
    var isAutofillEnabledAndSupportedStub: Boolean = false

    @ExperimentalCoroutinesApi
    open class FakeOnboardingStore(
        dispatcher: Dispatcher,
        dataStore: DataStore,
        fingerprintStore: FingerprintStore,
        autofillStore: AutofillStore,
        routeStore: RouteStore
    ) : OnboardingStore() {
//        val authStateStub = PublishSubject.create<FingerprintStore.AuthenticationState>()
//        override val authState: Observable<FingerprintStore.AuthenticationState>
//            get() = authStateStub
//
//        private val isFingerprintAuthAvailableStub: Boolean = true
//        override val isFingerprintAuthAvailable: Boolean
//            get() = isFingerprintAuthAvailableStub

        val onboardingStateStub = PublishSubject.create<Boolean>()
        override val onboarding: Observable<Boolean>
            get() = onboardingStateStub

        override var triggerFingerprintAuthOnboarding: Boolean = false
        override var triggerAutofillOnboarding: Boolean = false

    }

    val subject = FakeOnboardingStore(dispatcher, dataStore, fingerprintStore, autofillStore, routeStore)

    @Before
    fun setUp() {
        dispatcher.register.subscribe { dispatcherObserver }
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(isFingerprintAuthAvailableStub)
        Mockito.`when`(autofillStore.isAutofillEnabledAndSupported).thenReturn(isAutofillEnabledAndSupportedStub)
    }

    @Test
    fun `on onboarding check if fingerprint is enabled`() {
        subject.onboardingStateStub.onNext(true)
        isAutofillEnabledAndSupportedStub = true
        isFingerprintAuthAvailableStub = true

        Assert.assertEquals(true, subject.triggerAutofillOnboarding)
        Assert.assertEquals(true, subject.triggerFingerprintAuthOnboarding)
    }
}
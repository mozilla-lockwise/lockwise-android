/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.FingerprintStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class WelcomePresenterTest {
    class FakeWelcomeView : WelcomeView {
        var existingAccount: Boolean? = null

        override fun showExistingAccount(email: String) {
            existingAccount = true
        }

        override fun hideExistingAccount() {
            existingAccount = false
        }

        val learnMoreStub = PublishSubject.create<Unit>()
        override val learnMoreClicks: Observable<Unit> = learnMoreStub

        val getStartedStub: PublishSubject<Unit> = PublishSubject.create<Unit>()
        override val getStartedManuallyClicks: Observable<Unit>
            get() = getStartedStub

        val getStartedExistingAcccountStub: PublishSubject<Unit> = PublishSubject.create<Unit>()
        override val getStartedAutomaticallyClicks: Observable<Unit>
            get() = getStartedExistingAcccountStub
    }

    @Mock
    val fingerprintStore = Mockito.mock(FingerprintStore::class.java)
    var isDeviceSecureStub: Boolean = false

    val view = FakeWelcomeView()

    @Mock
    val accountStore = Mockito.mock(AccountStore::class.java)

    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()

    val subject = WelcomePresenter(view, dispatcher,
        accountStore = accountStore,
        fingerprintStore = fingerprintStore
    )

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        Mockito.`when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(isDeviceSecureStub)
        Mockito.`when`(accountStore.shareableAccount()).thenReturn(null)
        subject.onViewReady()
        Assert.assertEquals(false, view.existingAccount)
    }

    @Test
    fun `get started clicks checks device security`() {
        isDeviceSecureStub = true
        view.getStartedStub.onNext(Unit)

        val routeAction = dispatcherObserver.values().first() as RouteAction
        Assert.assertTrue(routeAction is DialogAction.OnboardingSecurityDialogManual)
    }

    @Test
    fun `learn more clicks`() {
        view.learnMoreStub.onNext(Unit)
        dispatcherObserver.assertValue(AppWebPageAction.FaqWelcome)
    }
}

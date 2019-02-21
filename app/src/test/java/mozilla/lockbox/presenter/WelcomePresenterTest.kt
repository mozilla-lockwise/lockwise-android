/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class WelcomePresenterTest {
    class FakeView : WelcomeView {
        val learnMoreStub = PublishSubject.create<Unit>()
        override val learnMoreClicks: Observable<Unit> = learnMoreStub

        val getStartedStub: PublishSubject<Unit> = PublishSubject.create<Unit>()
        override val getStartedClicks: Observable<Unit>
            get() = getStartedStub
    }

    val view = FakeView()
    val dispatcher = Dispatcher()

    val dispatcherObserver = TestObserver.create<Action>()

    val subject = WelcomePresenter(view, dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `get started clicks`() {
        view.getStartedStub.onNext(Unit)

        dispatcherObserver.assertValue(RouteAction.Login)
    }

    @Test
    fun `learn more clicks`() {
        view.learnMoreStub.onNext(Unit)

        dispatcherObserver.assertValue(RouteAction.AppWebPage.FaqWelcome)
    }
}
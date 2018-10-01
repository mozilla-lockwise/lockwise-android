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
import org.junit.Test

class BackablePresenterTest {
    class FakeView : BackableViewProtocol {
        val tapStub: PublishSubject<Unit> = PublishSubject.create<Unit>()

        override val backButtonTaps: Observable<Unit>
            get() = tapStub
    }

    val view = FakeView()
    val dispatcher = Dispatcher()
    val subject = BackablePresenter(view, dispatcher)

    @Test
    fun onViewReady() {
        val testObserver = TestObserver.create<Action>()

        val subscription = dispatcher.register.subscribeWith(testObserver)

        subject.onViewReady()
        view.tapStub.onNext(Unit)

        testObserver.assertValue(RouteAction.Back)

        subscription.dispose()
    }
}
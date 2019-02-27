/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class AutofillFilterPresenterTest {
    class FakeView : AutofillFilterView {
        val fillMeButtonStub = PublishSubject.create<Unit>()
        override val fillMeButtonClicks: Observable<Unit>
            get() = fillMeButtonStub
    }

    private val view = FakeView()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()

    val subject = AutofillFilterPresenter(view, dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `fillme button clicks`() {
        view.fillMeButtonStub.onNext(Unit)

        dispatcherObserver.assertValue { it is AutofillAction.Complete }
    }
}
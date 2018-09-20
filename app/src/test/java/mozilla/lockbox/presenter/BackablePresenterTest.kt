package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class BackablePresenterTest {
    class FakeView : BackableViewProtocol {
        val tapStub : PublishSubject<Unit> = PublishSubject.create<Unit>()

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

        testObserver.assertValue(RouteAction.BACK)

        subscription.dispose()
    }
}
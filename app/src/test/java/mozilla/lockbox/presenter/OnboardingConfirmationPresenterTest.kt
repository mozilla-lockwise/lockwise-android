package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class OnboardingConfirmationPresenterTest {
    class FakeOnboardingConfirmationView : OnboardingConfirmationView {
        override val finishClicks: Observable<Unit> = PublishSubject.create()
    }

    val view = FakeOnboardingConfirmationView()
    val dispatcher = Dispatcher()

    val dispatcherObserver = TestObserver.create<Action>()

    val subject = OnboardingConfirmationPresenter(view, dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun finishClicks() {
        (view.finishClicks as Subject).onNext(Unit)

        dispatcherObserver.assertLastValue(OnboardingStatusAction(false))
    }
}
package mozilla.lockbox.presenter

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class OnboardingAutofillPresenterTest {

    open class FakeView : OnboardingAutofillView {
        override val onEnable = PublishSubject.create<Unit>()
        override val onDismiss = PublishSubject.create<Unit>()
    }

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    private val view = Mockito.spy(OnboardingAutofillPresenterTest.FakeView())

    val subject = OnboardingAutofillPresenter(view, dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `dismiss dialog when skip is tapped`() {
        view.onDismiss.onNext(Unit)
        dispatcherObserver.assertValue(RouteAction.Onboarding.SkipOnboarding)
    }

    @Test
    fun `navigate to settings when button is tapped`() {
        view.onDismiss.onNext(Unit)
        dispatcherObserver.assertValueAt(0, RouteAction.SettingList)
    }
}
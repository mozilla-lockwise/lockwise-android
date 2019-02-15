package mozilla.lockbox.presenter

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class OnboardingSecurityDialogPresenterTest {

    open class FakeDialogView : OnboardingSecurityDialogView {
        override val onSkipClick = PublishSubject.create<Unit>()
        override val onSetUpNowClick = PublishSubject.create<Unit>()
    }

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    private val view = spy(FakeDialogView())
    val subject = OnboardingSecurityDialogPresenter(view, dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `dismiss on skip click`() {
        view.onSkipClick.onNext(Unit)
        dispatcherObserver.assertValue(RouteAction.Login)
    }

    @Test
    fun `dismiss and navigate to settings on set up now click`() {
        view.onSetUpNowClick.onNext(Unit)
        dispatcherObserver.assertValueAt(0, RouteAction.SystemSetting(SettingIntent.Security))
        dispatcherObserver.assertValueAt(1, RouteAction.Login)
    }
}

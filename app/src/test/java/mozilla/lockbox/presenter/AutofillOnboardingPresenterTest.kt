package mozilla.lockbox.presenter

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class AutofillOnboardingPresenterTest {

    open class FakeView : AutofillOnboardingView {
        override val onGoToSettingsClick = PublishSubject.create<Unit>()
        override val onSkipClick = PublishSubject.create<Unit>()
    }

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    private val view = Mockito.spy(AutofillOnboardingPresenterTest.FakeView())

    val subject = AutofillOnboardingPresenter(view, dispatcher)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `move to next screen when skip is tapped`() {
        view.onSkipClick.onNext(Unit)
        dispatcherObserver.assertValue(RouteAction.Onboarding.Confirmation)
    }

    @Test
    fun `navigate to settings when gotosettings button is tapped`() {
        view.onGoToSettingsClick.onNext(Unit)
        dispatcherObserver.assertValueAt(0, RouteAction.SystemSetting(SettingIntent.Autofill))
        dispatcherObserver.assertValueAt(1, RouteAction.Onboarding.Confirmation)
    }
}
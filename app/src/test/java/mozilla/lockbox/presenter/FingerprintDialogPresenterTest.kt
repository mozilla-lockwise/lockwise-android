package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.FingerprintStore.AuthenticationState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class FingerprintDialogPresenterTest {

    open class FakeView : FingerprintDialogView {
        override fun onSucceeded() {
        }

        override fun onFailed(error: String?) {
        }

        override fun onError(error: String?) {
        }

        val authCallbackStub = PublishSubject.create<FingerprintAuthAction>()
        override val authCallback: Observable<FingerprintAuthAction>
            get() = authCallbackStub

        override val onDismiss = PublishSubject.create<Unit>()
    }

    open class FakeFingerprintStore : FingerprintStore() {
        val authStateStub = PublishSubject.create<AuthenticationState>()
        override val authState: Observable<AuthenticationState>
            get() = authStateStub
    }

    val dispatcher = Dispatcher()
    private val view = spy(FakeView())
    private val fingerprintStore = spy(FakeFingerprintStore())
    private val dispatcherObserver = TestObserver.create<Action>()
    val subject = FingerprintDialogPresenter(view, dispatcher, fingerprintStore)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `update on succeeded state`() {
        fingerprintStore.authStateStub.onNext(AuthenticationState.Succeeded)
        verify(view).onSucceeded()
    }

    @Test
    fun `update on failed state`() {
        fingerprintStore.authStateStub.onNext(AuthenticationState.Failed("error"))
        verify(view).onFailed("error")
    }

    @Test
    fun `update on error state`() {
        fingerprintStore.authStateStub.onNext(AuthenticationState.Error("error"))
        verify(view).onError("error")
    }

    @Test
    fun `dispatch authentication status for routing`() {
        view.authCallbackStub.onNext(FingerprintAuthAction.OnSuccess)
        dispatcherObserver.assertLastValue(FingerprintAuthAction.OnSuccess)
    }

    @Test
    fun `dismiss dialog on cancel tapped`() {
        view.onDismiss.onNext(Unit)
        dispatcherObserver.assertValue(FingerprintAuthAction.OnCancel)
    }

    @Test
    fun `should start listening on resume`() {
        subject.onResume()
        dispatcherObserver.assertValue(FingerprintSensorAction.Start)
    }

    @Test
    fun `should stop listening on pause`() {
        subject.onPause()
        dispatcherObserver.assertLastValue(FingerprintSensorAction.Stop)
    }
}

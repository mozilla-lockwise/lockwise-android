package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class FingerprintDialogPresenterTest {

    open class FakeView : FingerprintDialogView {
        override fun onSucceeded() {
        }

        override fun onFailed(error: String?) {
        }

        override fun onError(error: String?) {
        }

        val authCallbackStub = PublishSubject.create<FingerprintAuthDialogFragment.AuthCallback>()
        override val authCallback: Observable<FingerprintAuthDialogFragment.AuthCallback>
            get() = authCallbackStub

        override val onDismiss = PublishSubject.create<Unit>()
    }

    open class FakeFingerprintStore : FingerprintStore() {
        val authStateStub = PublishSubject.create<AuthenticationState>()
        override val authState: Observable<AuthenticationState>
            get() = authStateStub
    }

    private val view = spy(FakeView())
    private val fingerprintStore = spy(FakeFingerprintStore())
    private val dispatcherObserver = TestObserver.create<Action>()
    val subject = FingerprintDialogPresenter(view, Dispatcher.shared, fingerprintStore)

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `update on succeeded state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Succeeded)
        verify(view).onSucceeded()
    }

    @Test
    fun `update on failed state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Failed("error"))
        verify(view).onFailed("error")
    }

    @Test
    fun `update on error state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Error("error"))
        verify(view).onError("error")
    }

    @Test
    fun `dispatch authentication status for routing`() {
        view.authCallbackStub.onNext(FingerprintAuthDialogFragment.AuthCallback.OnAuth)
        dispatcherObserver.assertLastValue(FingerprintAuthAction.OnAuthentication(FingerprintAuthDialogFragment.AuthCallback.OnAuth))
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

package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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

        override val cancelTapped = PublishSubject.create<Unit>()

        override fun onCancel() {
        }
    }

    open class FakeFingerprintStore : FingerprintStore() {
        val authStateStub = PublishSubject.create<AuthenticationState>()
        override val authState: Observable<AuthenticationState>
            get() = authStateStub
    }
    @Mock
    val keyguardManager = Mockito.mock(KeyguardManager::class.java)

    val view = spy(FakeView())
    val fingerprintStore = spy(FakeFingerprintStore())
    val dispatcherObserver = TestObserver.create<Action>()
    val subject = FingerprintDialogPresenter(view, Dispatcher.shared, fingerprintStore)

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    private val fingerprintManager =
        RuntimeEnvironment.application.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

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
}

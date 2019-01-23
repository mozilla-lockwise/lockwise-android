package mozilla.lockbox.presenter

import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject.create
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.RouteStore
import org.junit.Before
import org.junit.Test
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment.AuthCallback as AuthCallback
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class OnboardingFingerprintAuthPresenterTest {


    open class FakeView : OnboardingFingerprintView {
        override fun onSucceeded() {
        }

        override fun onFailed(error: String?) {
        }

        override fun onError(error: String?) {
        }


        override val onDismiss = create<Unit>()

        val authCallbackStub = create<AuthCallback>()
        override val authCallback: Observable<AuthCallback>
            get() = authCallbackStub

    }

    open class FakeFingerprintStore : FingerprintStore() {
        val authStateStub = create<FingerprintStore.AuthenticationState>()
        override val authState: Observable<FingerprintStore.AuthenticationState>
            get() = authStateStub
    }

    @Mock
    private val routeStore = PowerMockito.mock(RouteStore::class.java)

    val dispatcher = Dispatcher()
    private val view = Mockito.spy(FakeView())
    private val fingerprintStore = Mockito.spy(FakeFingerprintStore())

    @Mock
    private val fingerprintManager = PowerMockito.mock(FingerprintManager::class.java)

    private val dispatcherObserver = TestObserver.create<Action>()

    val subject = OnboardingFingerprintAuthPresenter(view, dispatcher, routeStore, fingerprintStore)

    @Before
    fun setUp() {
        fingerprintStore.fingerprintManager = fingerprintManager
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }


    @Test
    fun `update on succeeded state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Succeeded)
        Mockito.verify(view).onSucceeded()
    }

    @Test
    fun `update on failed state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Failed("error"))
        Mockito.verify(view).onFailed("error")
    }


    @Test
    fun `update on error state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Error("error"))
        Mockito.verify(view).onError("error")
    }


    @Test
    fun `dismiss dialog when skip is tapped`() {
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
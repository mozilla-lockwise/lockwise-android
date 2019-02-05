package mozilla.lockbox.presenter

import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.RouteStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment.AuthCallback as AuthCallback
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenCalled

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class OnboardingFingerprintAuthPresenterTest {

    open class FakeView : OnboardingFingerprintView {

        var success: Boolean = false
        var errors: Boolean = false
        var failure: Boolean = false
        // probably a better way to do this...
        override fun onSucceeded() {
            success = true
        }

        override fun onFailed(error: String?) {
            failure = true
        }

        override fun onError(error: String?) {
            errors = true
        }

        private var authCallbackStub = PublishSubject.create<AuthCallback>()
        override val authCallback: Observable<AuthCallback>
            get() = authCallbackStub

        override val onDismiss = PublishSubject.create<Unit>()
    }

    open class FakeFingerprintStore : FingerprintStore() {
        val authStateStub = PublishSubject.create<FingerprintStore.AuthenticationState>()
        override val authState: Observable<FingerprintStore.AuthenticationState>
            get() = authStateStub

        private val isFingerprintAuthAvailableStub: Boolean = true
        override val isFingerprintAuthAvailable: Boolean
            get() = isFingerprintAuthAvailableStub
    }

    @Mock
    private val routeStore = PowerMockito.mock(RouteStore::class.java)
    private var onboardingStub = Observable.just(true)
    val dispatcher = Dispatcher()
    private val view = Mockito.spy(FakeView())

    private val fingerprintStore = FakeFingerprintStore()

    @Mock
    private val fingerprintManager = PowerMockito.mock(FingerprintManager::class.java)

    private val dispatcherObserver = TestObserver.create<Action>()

    val subject = OnboardingFingerprintAuthPresenter(view, dispatcher, fingerprintStore)

    @Before
    fun setUp() {
        fingerprintStore.fingerprintManager = fingerprintManager
        whenCalled(routeStore.onboarding).thenReturn(onboardingStub)
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `update on succeeded state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Succeeded)
        Assert.assertEquals(true, view.success)
    }

    @Test
    fun `update on failed state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Failed("error"))
        Assert.assertEquals(true, view.failure)
    }

    @Test
    fun `update on error state`() {
        fingerprintStore.authStateStub.onNext(FingerprintStore.AuthenticationState.Error("error"))
        Assert.assertEquals(true, view.errors)
    }

    @Test
    fun `dismiss dialog when skip is tapped`() {
        view.onDismiss.onNext(Unit)
        dispatcherObserver.assertValueAt(0, RouteAction.Onboarding.SkipOnboarding)
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
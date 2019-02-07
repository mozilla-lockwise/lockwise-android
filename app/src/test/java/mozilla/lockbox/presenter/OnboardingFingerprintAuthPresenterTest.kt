package mozilla.lockbox.presenter

import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.store.FingerprintStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
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
        var success: Boolean = false
        var errors: Boolean = false
        var failure: Boolean = false

        override fun onSucceeded() {
            success = true
        }

        override fun onFailed(error: String?) {
            failure = true
        }

        override fun onError(error: String?) {
            errors = true
        }

        private var authCallbackStub = PublishSubject.create<FingerprintAuthCallback>()
        override val authCallback: Observable<FingerprintAuthCallback>
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
        dispatcherObserver.assertValueAt(0, OnboardingStatusAction(false))
        dispatcherObserver.assertValueAt(1, SettingAction.UnlockWithFingerprint(false))
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
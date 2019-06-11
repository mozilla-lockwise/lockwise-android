@file:Suppress("DEPRECATION")

package mozilla.lockbox.presenter

import android.view.autofill.AutofillManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.FingerprintSensorAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@Config(application = TestApplication::class)
class FingerprintOnboardingPresenterTest {
    open class FakeOnboardingView : FingerprintOnboardingView {
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

        private var authCallbackStub = PublishSubject.create<FingerprintAuthAction>()
        override val authCallback: Observable<FingerprintAuthAction>
            get() = authCallbackStub

        override val onSkipClick = PublishSubject.create<Unit>()
    }

    val authStateStub = PublishSubject.create<FingerprintStore.AuthenticationState>()

    val dispatcher = Dispatcher()
    private val view = Mockito.spy(FakeOnboardingView())

    @Mock
    val fingerprintStore = PowerMockito.mock(FingerprintStore::class.java)

    @Mock
    private val autofillManager = PowerMockito.mock(AutofillManager::class.java)
    private val autofillAvailableStub: Boolean = true
    private val hasEnabledAutofillServicesStub: Boolean = true

    private val dispatcherObserver = TestObserver.create<Action>()

    lateinit var subject: FingerprintOnboardingPresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        whenCalled(autofillManager.isAutofillSupported).thenReturn(autofillAvailableStub)
        whenCalled(autofillManager.hasEnabledAutofillServices()).thenReturn(hasEnabledAutofillServicesStub)
        whenCalled(fingerprintStore.authState).thenReturn(authStateStub)
        PowerMockito.whenNew(FingerprintStore::class.java).withAnyArguments().thenReturn(fingerprintStore)
        subject = FingerprintOnboardingPresenter(view, dispatcher, fingerprintStore)
        subject.onViewReady()
    }

    @Test
    fun `update on succeeded state`() {
        authStateStub.onNext(FingerprintStore.AuthenticationState.Succeeded)
        Assert.assertEquals(true, view.success)
    }

    @Test
    fun `update on failed state`() {
        authStateStub.onNext(FingerprintStore.AuthenticationState.Failed("error"))
        Assert.assertEquals(true, view.failure)
    }

    @Test
    fun `update on error state`() {
        authStateStub.onNext(FingerprintStore.AuthenticationState.Error("error"))
        Assert.assertEquals(true, view.errors)
    }

    @Test
    fun `move to next screen when skip is tapped`() {
        view.onSkipClick.onNext(Unit)
        dispatcherObserver.assertValueAt(0, SettingAction.UnlockWithFingerprint(false))
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
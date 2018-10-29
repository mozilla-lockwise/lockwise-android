package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment.AuthCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class LockedPresenterTest {

    open class FakeView : LockedView {
        override fun unlockFallback(manager: KeyguardManager) {
        }

        val unlockConfirmedStub = PublishSubject.create<Boolean>()
        override val unlockConfirmed: Observable<Boolean> get() = unlockConfirmedStub
        override val unlockButtonTaps = PublishSubject.create<Unit>()
    }

    open class FakeFingerprintStore : FingerprintStore()

    class FakeLockedStore : LockedStore() {
        val onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = onAuth
    }

    val view = spy(FakeView())
    val fingerprintStore = spy(FakeFingerprintStore())
    val lockedStore = FakeLockedStore()
    val dispatcherObserver = TestObserver.create<Action>()
    val subject = LockedPresenter(view, Dispatcher.shared, fingerprintStore, lockedStore)

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)
        fingerprintStore.apply(fingerprintManager)
        lockedStore.apply(keyguradManager)
        subject.onViewReady()
    }

    @Test
    fun `unlock button tap shows fingerprint dialog`() {
        `when`(fingerprintStore.isFingerprintAuthAvailable()).thenReturn(true)
        view.unlockButtonTaps.onNext(Unit)
        dispatcherObserver.assertLastValue(RouteAction.FingerprintDialog)
    }

    @Test
    fun `unlock button tap fallback if no fingerprint`() {
        `when`(fingerprintStore.isFingerprintAuthAvailable()).thenReturn(false)
        `when`(keyguradManager.isKeyguardSecure).thenReturn(true)
        view.unlockButtonTaps.onNext(Unit)
        verify(view).unlockFallback(keyguradManager)
    }

    @Test
    fun `handle success authentication callback`() {
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnAuthentication(AuthCallback.OnAuth))
        dispatcherObserver.assertLastValue(RouteAction.ItemList)
    }

    @Test
    fun `handle error authentication callback`() {
        `when`(lockedStore.isKeyguardSecure).thenReturn(false)
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnAuthentication(AuthCallback.OnError))
        dispatcherObserver.assertLastValue(RouteAction.LockScreen)
    }

    @Test
    fun `handle unlock confirmed true`() {
        view.unlockConfirmedStub.onNext(true)
        dispatcherObserver.assertLastValue(RouteAction.ItemList)
    }

    @Test
    fun `handle unlock confirmed false`() {
        view.unlockConfirmedStub.onNext(false)
        dispatcherObserver.assertLastValue(RouteAction.LockScreen)
    }

    private val fingerprintManager =
        RuntimeEnvironment.application.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

    private val keyguradManager =
        spy(RuntimeEnvironment.application.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
}
package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
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
        override fun unlockFallback() {
        }

        val unlockConfirmedStub = PublishSubject.create<Boolean>()
        override val unlockConfirmed: Observable<Boolean> get() = unlockConfirmedStub
        override val unlockButtonTaps = PublishSubject.create<Unit>()
    }

    open class FakeFingerprintStore : FingerprintStore() {
        override var fingerprintManager: FingerprintManager =
            RuntimeEnvironment.application.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

        override var keyguardManager: KeyguardManager =
            spy(RuntimeEnvironment.application.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
    }

    class FakeLockedStore : LockedStore() {
        val onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = onAuth

        override val canLaunchAuthenticationOnForeground: Observable<Boolean> = PublishSubject.create()
    }

    class FakeSettingStore : SettingStore() {
        val unlock = PublishSubject.create<Boolean>()
        override var unlockWithFingerprint: Observable<Boolean> = unlock
    }

    val view: FakeView = spy(FakeView())
    private val fingerprintStore: FakeFingerprintStore = spy(FakeFingerprintStore())
    private val lockedStore = FakeLockedStore()
    private val settingStore = FakeSettingStore()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private lateinit var context: Context
    val subject = LockedPresenter(
        view,
        dispatcher,
        fingerprintStore,
        lockedStore,
        settingStore
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application.applicationContext
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `unlock button tap shows fingerprint dialog`() {
        `when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        view.unlockButtonTaps.onNext(Unit)
        assertTrue((dispatcherObserver.values().first() as UnlockingAction).currently)
        settingStore.unlock.onNext(true)
        val routeAction = dispatcherObserver.values().last() as RouteAction.DialogFragment
        assertTrue(routeAction is RouteAction.DialogFragment.FingerprintDialog)
    }

    @Test
    fun `unlock button tap fallback if no fingerprint`() {
        `when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        `when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(true)
        view.unlockButtonTaps.onNext(Unit)
        settingStore.unlock.onNext(false)
        verify(view).unlockFallback()
    }

    @Test
    fun `unlock button tap fallback on fingerprint error`() {
        `when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        `when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(true)
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnError))
        verify(view).unlockFallback()
    }

    @Test
    fun `onviewready when can launch authentication shows fingerprint dialog`() {
        (lockedStore.canLaunchAuthenticationOnForeground as Subject).onNext(true)
        `when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        settingStore.unlock.onNext(true)
        val routeAction = dispatcherObserver.values().last() as RouteAction.DialogFragment
        assertTrue(routeAction is RouteAction.DialogFragment.FingerprintDialog)
    }

    @Ignore
    @Test
    fun `onviewready when can launch authentication if no fingerprint`() {
        (lockedStore.canLaunchAuthenticationOnForeground as Subject).onNext(true)

        `when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(false)
        `when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(true)
        settingStore.unlock.onNext(false)
        verify(view).unlockFallback()
    }

    @Test
    fun `foreground action fallback on fingerprint error`() {
        `when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        `when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(true)
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnError))
        verify(view).unlockFallback()
    }

    @Test
    fun `handle success authentication callback`() {
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnAuth))

        assertEquals(2, dispatcherObserver.valueCount())
        assertEquals(DataStoreAction.Unlock, dispatcherObserver.values().first())
        assertFalse((dispatcherObserver.values().last() as UnlockingAction).currently)
    }

    @Test
    fun `handle error authentication callback`() {
        `when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(false)
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnAuthentication(FingerprintAuthCallback.OnError))

        assertEquals(2, dispatcherObserver.valueCount())
        assertEquals(DataStoreAction.Unlock, dispatcherObserver.values().first())
        assertFalse((dispatcherObserver.values().last() as UnlockingAction).currently)
    }

    @Test
    fun `handle unlock confirmed true`() {
        view.unlockConfirmedStub.onNext(true)

        assertEquals(2, dispatcherObserver.valueCount())
        assertEquals(DataStoreAction.Unlock, dispatcherObserver.values().first())
        assertFalse((dispatcherObserver.values().last() as UnlockingAction).currently)
    }

    @Test
    fun `handle unlock confirmed false`() {
        view.unlockConfirmedStub.onNext(false)
        Assert.assertEquals(0, dispatcherObserver.valueCount())
    }
}
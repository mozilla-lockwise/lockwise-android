/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class AutofillLockedPresenterTest : DisposingTest() {

    open class FakeView : LockedView {
        override val unlockButtonTaps: Observable<Unit>?
            get() = TODO("not implemented")

        val unlockConfirmedStub = PublishSubject.create<Boolean>()
        var unlockFallbackCalled: Boolean = false

        override val unlockConfirmed: Observable<Boolean> get() = unlockConfirmedStub
        override fun unlockFallback() {
            unlockFallbackCalled = true
        }
    }

    open class FakeFingerprintStore : FingerprintStore() {
        override var fingerprintManager: FingerprintManager? =
            RuntimeEnvironment.application.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager

        override var keyguardManager: KeyguardManager =
            Mockito.spy(RuntimeEnvironment.application.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
    }

    class FakeLockedStore : LockedStore() {
        val onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = onAuth

        override val canLaunchAuthenticationOnForeground: Observable<Boolean> = PublishSubject.create()
    }

    class FakeSettingStore : SettingStore() {
        val unlockWithFingerprintStub = PublishSubject.create<Boolean>()
        override var unlockWithFingerprint: Observable<Boolean> = unlockWithFingerprintStub
    }

    val view = Mockito.spy(FakeView())
    private val fingerprintStore = Mockito.spy(FakeFingerprintStore())
    private val lockedStore = FakeLockedStore()
    private val settingStore = FakeSettingStore()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()
    private lateinit var context: Context

    val subject = AutofillLockedPresenter(view)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application.applicationContext
        fingerprintStore.injectContext(context)
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `pushes autofill cancel action on unlockConfirmed(false)`() {
        view.unlockConfirmedStub.onNext(false)
        dispatcherObserver.assertLastValue(AutofillAction.Cancel)
    }

    @Test
    fun `pushes unlock action on unlockConfirmed(true)`() {
        view.unlockConfirmedStub.onNext(true)
        dispatcherObserver.assertValueAt(0, DataStoreAction.Unlock)
    }

    @Test
    fun `pushes autofill cancel action to finish with null on fingerprint auth canceled`() {
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnCancel)
        dispatcherObserver.assertLastValue(AutofillAction.Cancel)
    }

    @Test
    fun `unlock on successful fingerprint authentication`() {
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnSuccess)
        dispatcherObserver.assertValueAt(0, DataStoreAction.Unlock)
    }

    @Test
    fun `unlock when fingerprint auth is available and enabled`() {
        Mockito.`when`(fingerprintStore.isFingerprintAuthAvailable).thenReturn(true)
        Mockito.`when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(true)

        val dispatchIterator = dispatcher.register.blockingIterable().iterator()
        settingStore.unlockWithFingerprintStub.onNext(true)
        val routeAction = dispatchIterator.next()

        Assert.assertTrue(routeAction is RouteAction.DialogFragment.FingerprintDialog)
    }
}
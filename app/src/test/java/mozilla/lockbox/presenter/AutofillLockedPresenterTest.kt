/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.R
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class AutofillLockedPresenterTest : DisposingTest() {
    class FakeLockedStore : LockedStore() {
        internal val authenticationStub = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = authenticationStub
    }

    class FakeAutofillView : AutofillLockedView {
        var unlockFallbackCalled = false
        override fun unlockFallback() {
            unlockFallbackCalled = true
        }

        internal val _onUnlockConfirm = PublishSubject.create<Boolean>()
        override val unlockConfirmed: Observable<Boolean>
            get() = _onUnlockConfirm
    }

    class FakeSettingStore : SettingStore() {
        val unlockWithFingerprintStub = PublishSubject.create<Boolean>()
        override val unlockWithFingerprint: Observable<Boolean>
            get() = unlockWithFingerprintStub
    }

    @Mock
    val fingerprintManager = Mockito.mock(FingerprintManager::class.java)

    @Mock
    val keyguardManager = Mockito.mock(KeyguardManager::class.java)

    @Mock
    val context = Mockito.mock(Context::class.java)

    class FakeFingerprintStore : FingerprintStore() {
        var fingerprintAuthAvailableStub: Boolean = false
        override val isFingerprintAuthAvailable: Boolean
            get() = fingerprintAuthAvailableStub

        var keyguardDeviceSecureStub: Boolean = false
        override val isKeyguardDeviceSecure: Boolean
            get() = keyguardDeviceSecureStub
    }

    private val dispatcher = Dispatcher()
    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()
    private val lockedStore = FakeLockedStore()
    private val fingerprintStore = FakeFingerprintStore()
    private val settingStore = FakeSettingStore()
    private val view = FakeAutofillView()

    val subject = AutofillLockedPresenter(
            view,
            dispatcher,
            fingerprintStore,
            settingStore,
            lockedStore
        )

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

    }

    @Test
    fun `pushes autofill cancel action on unlockConfirmed(false)`() {
        view._onUnlockConfirm.onNext(false)
        dispatcherObserver.assertLastValue(AutofillAction.Cancel)
    }

    @Test
    fun `pushes unlock action on unlockConfirmed(true)`() {
        view._onUnlockConfirm.onNext(true)
        dispatcherObserver.assertLastValue(DataStoreAction.Unlock)
    }

    @Test
    fun `pushes autofill cancel action to finish with null on fingerprint auth canceled`() {
        lockedStore.authenticationStub.onNext(FingerprintAuthAction.OnCancel)
        dispatcherObserver.assertLastValue(AutofillAction.Cancel)
    }

    @Test
    fun `on successful fingerprint authentication`() {
        lockedStore.authenticationStub.onNext(FingerprintAuthAction.OnSuccess)
        dispatcherObserver.assertLastValue(DataStoreAction.Unlock)
    }

    @Test
    fun `on unsuccessful fingerprint authentication when there is no other security`() {
//        fingerprintStore.keyguardDeviceSecureStub = false
        lockedStore.authenticationStub.onNext(FingerprintAuthAction.OnError)
        dispatcherObserver.assertLastValue(DataStoreAction.Unlock)
    }

    @Test
    fun `on unsuccessful fingerprint authentication when there is other security`() {
        fingerprintStore.keyguardDeviceSecureStub = true

        Mockito.`when`(context.getSystemService(Context.FINGERPRINT_SERVICE)).thenReturn(fingerprintManager)
        Mockito.`when`(context.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager)

        fingerprintStore.injectContext(context)
//        Assert.assertTrue(keyguardManager.isDeviceSecure)

        Mockito.`when`(fingerprintStore.keyguardManager).thenReturn(keyguardManager)

        Mockito.`when`(fingerprintManager.isHardwareDetected).thenReturn(true)
        Mockito.`when`(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true)
        Mockito.`when`(keyguardManager.isDeviceSecure).thenReturn(true)
//        Mockito.`when`(fingerprintStore.isKeyguardDeviceSecure).thenReturn(true)

        subject.onViewReady()

        lockedStore.authenticationStub.onNext(FingerprintAuthAction.OnError)
        assertTrue(view.unlockFallbackCalled)
    }

    @Test
    fun `when fingerprint auth is available and enabled`() {
//        fingerprintStore.fingerprintAuthAvailableStub = true
        settingStore.unlockWithFingerprintStub.onNext(true)

        Thread.sleep(600)
        val fingerprintDialog = dispatcherObserver.values()[0] as RouteAction.DialogFragment.FingerprintDialog
        assertEquals(R.string.fingerprint_dialog_title, fingerprintDialog.dialogTitle)
    }

    @Test
    fun `when fingerprint auth is available but not enabled and there is other device security`() {
//        fingerprintStore.fingerprintAuthAvailableStub = true
//        fingerprintStore.keyguardDeviceSecureStub = true
        settingStore.unlockWithFingerprintStub.onNext(false)
        Thread.sleep(600)
        assertTrue(view.unlockFallbackCalled)
    }

    @Test
    fun `when fingerprint auth is not available or enabled and there is other device security`() {
//        fingerprintStore.fingerprintAuthAvailableStub = false
//        fingerprintStore.keyguardDeviceSecureStub = true
        settingStore.unlockWithFingerprintStub.onNext(false)
        Thread.sleep(600)
        assertTrue(view.unlockFallbackCalled)
    }

    @Test
    fun `when fingerprint auth is available but not enabled and there is not other device security`() {
//        fingerprintStore.fingerprintAuthAvailableStub = true
//        fingerprintStore.keyguardDeviceSecureStub = false
        settingStore.unlockWithFingerprintStub.onNext(false)
        Thread.sleep(600)
        dispatcherObserver.assertLastValue(DataStoreAction.Unlock)
    }

    @Test
    fun `when fingerprint auth is not available or enabled and there is not other device security`() {
//        fingerprintStore.fingerprintAuthAvailableStub = false
//        fingerprintStore.keyguardDeviceSecureStub = false
        settingStore.unlockWithFingerprintStub.onNext(false)
        Thread.sleep(600)
        dispatcherObserver.assertLastValue(DataStoreAction.Unlock)
    }
}
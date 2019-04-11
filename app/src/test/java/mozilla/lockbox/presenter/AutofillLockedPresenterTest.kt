/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.presenter

import android.app.Activity
import android.app.Activity.RESULT_OK
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
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.assertLastValueMatches
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class AutofillLockedPresenterTest : DisposingTest() {

    open class FakeView : LockedView {
        open var _unlockButtonTaps = Observable.just(Unit)
        override val unlockButtonTaps: Observable<Unit>?
            get() = _unlockButtonTaps

        val activityResult = PublishSubject.create<Pair<Int, Int>>()
        override val onActivityResult: Observable<Pair<Int, Int>>
            get() = activityResult
    }
//
//    open class FakeFingerprintStore : FingerprintStore() {
//        override var fingerprintManager: FingerprintManager? =
//            RuntimeEnvironment.application.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
//
//        override var keyguardManager: KeyguardManager =
//            Mockito.spy(RuntimeEnvironment.application.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
//    }
//
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

    val view = FakeView()

    @Mock
    private val fingerprintStore = Mockito.mock(FingerprintStore::class.java)
    private val settingStore = FakeSettingStore()
    private val lockedStore = FakeLockedStore()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()
//    private lateinit var context: Context

    val subject = AutofillLockedPresenter(view, dispatcher, fingerprintStore, lockedStore, settingStore)

    @Before
    fun setUp() {
//        context = RuntimeEnvironment.application.applicationContext
//        fingerprintStore.injectContext(context)
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `pushes autofill cancel action on unlockConfirmed(false)`() {
        view.activityResult.onNext(Pair(111, Activity.RESULT_CANCELED))
        dispatcherObserver.assertValueAt(0, AutofillAction.Cancel)
    }

    @Test
    fun `pushes unlock action on unlockConfirmed(true)`() {
        view.activityResult.onNext(Pair(mozilla.lockbox.support.Constant.RequestCode.unlock, RESULT_OK))
        dispatcherObserver.assertValueAt(0, DataStoreAction.Unlock)
    }

    @Test
    fun `pushes autofill cancel action to finish with null on fingerprint auth canceled`() {
        lockedStore.onAuth.onNext(FingerprintAuthAction.OnCancel)
        dispatcherObserver.assertValueAt(0, AutofillAction.Cancel)
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

        settingStore.unlockWithFingerprintStub.onNext(true)
        dispatcherObserver.assertValueCount(1)
    }
}
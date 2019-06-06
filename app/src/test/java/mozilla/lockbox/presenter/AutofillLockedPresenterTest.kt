/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.robolectric.annotation.Config

@Config(packageName = "mozilla.lockbox")
class AutofillLockedPresenterTest : DisposingTest() {

    private val unlockWithFingerprintStub = ReplaySubject.create<Boolean>()

    @Mock
    val settingStore = PowerMockito.mock(SettingStore::class.java)!!

    @Mock
    val view: LockedView = Mockito.mock(LockedView::class.java)

    @Mock
    private val fingerprintStore = Mockito.mock(FingerprintStore::class.java)

    @Mock
    private val lockedStore = Mockito.mock(LockedStore::class.java)

    private val dispatcher = Dispatcher()

    class AutofillLockedPresenterFake(
        lockedView: LockedView,
        override val dispatcher: Dispatcher,
        override val fingerprintStore: FingerprintStore,
        override val lockedStore: LockedStore,
        override val settingStore: SettingStore
    ) : AutofillLockedPresenter(lockedView, dispatcher, fingerprintStore, lockedStore, settingStore) {
        fun callUnlockAuthObs(): Observable<Boolean> {
            return Observable.just(Unit).unlockAuthenticationObservable()
        }
    }

    lateinit var subject: AutofillLockedPresenterFake

    @Before
    fun setUp() {
        whenCalled(settingStore.unlockWithFingerprint).thenReturn(unlockWithFingerprintStub)

        PowerMockito.whenNew(FingerprintStore::class.java).withAnyArguments().thenReturn(fingerprintStore)
        PowerMockito.whenNew(SettingStore::class.java).withAnyArguments().thenReturn(settingStore)

        subject = AutofillLockedPresenterFake(
            view,
            dispatcher,
            fingerprintStore,
            lockedStore,
            settingStore
        )
    }

    @Test
    fun `unlock when fingerprint auth is available and enabled`() {
        unlockWithFingerprintStub.onNext(true)
        val testObserver = subject.callUnlockAuthObs().test()
        testObserver.assertValue(true)
    }
}
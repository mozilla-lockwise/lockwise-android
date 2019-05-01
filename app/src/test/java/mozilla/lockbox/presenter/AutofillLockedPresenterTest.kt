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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "mozilla.lockbox")
class AutofillLockedPresenterTest : DisposingTest() {

    class FakeSettingStore : SettingStore() {
        val unlockWithFingerprintStub = ReplaySubject.create<Boolean>()
        override var unlockWithFingerprint: Observable<Boolean> = unlockWithFingerprintStub
    }

    @Mock
    val view = Mockito.mock(LockedView::class.java)

    @Mock
    private val fingerprintStore = Mockito.mock(FingerprintStore::class.java)

    @Mock
    private val lockedStore = Mockito.mock(LockedStore::class.java)

    private val settingStore = FakeSettingStore()
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

    val subject = AutofillLockedPresenterFake(
        view,
        dispatcher,
        fingerprintStore,
        lockedStore,
        settingStore
    )

    @Test
    fun `unlock when fingerprint auth is available and enabled`() {
        settingStore.unlockWithFingerprintStub.onNext(true)
        val testObserver = subject.callUnlockAuthObs().test()
        testObserver.assertValue(true)
    }
}
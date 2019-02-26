/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.store.SettingStore
import org.junit.Before
import org.junit.Test

internal class AutofillLockedPresenterTest : DisposingTest() {
    class FakeLockedStore : LockedStore() {
        internal val _onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = _onAuth
    }

    class FakeAutofillView : AutofillLockedView {
        override fun unlockFallback() {
            TODO("not implemented")
        }

        internal val _onUnlockConfirm = PublishSubject.create<Boolean>()
        override val unlockConfirmed: Observable<Boolean>
            get() = _onUnlockConfirm
    }

    private val dispatcher = Dispatcher()
    private val lockedStore = FakeLockedStore()
    private val fingerprintStore = FingerprintStore(dispatcher)
    private val settingStore = SettingStore(dispatcher)
    private val view = FakeAutofillView()

    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()

    val subject = AutofillLockedPresenter(view, dispatcher, fingerprintStore, settingStore, lockedStore)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    // TODO: Test the other paths

    @Test
    fun `pushes autofill cancel action to finish with null on unlockConfirmed(false)`() {
        view._onUnlockConfirm.onNext(false)
        dispatcherObserver.assertLastValue(AutofillAction.Cancel)
    }

    @Test
    fun `pushes autofill cancel action to finish with null on fingerprint auth canceled`() {
        lockedStore._onAuth.onNext(FingerprintAuthAction.OnCancel)
        dispatcherObserver.assertLastValue(AutofillAction.Cancel)
    }
}
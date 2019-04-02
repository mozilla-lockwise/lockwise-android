/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class LockedPresenterTest {

    open class FakeView : LockedView {
        val unlockConfirmedStub = PublishSubject.create<Boolean>()
        override val unlockConfirmed: Observable<Boolean> get() = unlockConfirmedStub
        override val unlockButtonTaps = PublishSubject.create<Unit>()
    }

    class FakeLockedPresenter(
        dispatcher: Dispatcher,
        fingerprintStore: FingerprintStore
    ) : LockedPresenter(dispatcher, fingerprintStore) {
        override fun unlock() {
            // TODO
        }
    }

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
    private val fingerprintStore = FakeFingerprintStore()
    val view: FakeView = Mockito.spy(FakeView())

    val subject = FakeLockedPresenter(dispatcher, fingerprintStore)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `handle unlock confirmed true`() {
        val dispatchIterator = dispatcher.register.blockingIterable().iterator()
        view.unlockConfirmedStub.onNext(true)

        Assert.assertEquals(DataStoreAction.Unlock, dispatchIterator.next())
        val unlockingAction = dispatchIterator.next() as UnlockingAction
        Assert.assertFalse(unlockingAction.currently)
    }
}
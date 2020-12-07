/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package mozilla.lockbox.presenter

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.support.Constant
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito

class LockedPresenterTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val trampoline = Schedulers.trampoline()
            RxJavaPlugins.setInitComputationSchedulerHandler { trampoline }
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { trampoline }
        }
    }

    class FakeLockedPresenter(
        view: FakeView,
        dispatcher: Dispatcher,
        fingerprintStore: FingerprintStore,
        lockedStore: LockedStore
    ) : LockedPresenter(view, dispatcher, fingerprintStore, lockedStore = lockedStore) {
        override fun Observable<Unit>.unlockAuthenticationObservable(): Observable<Boolean> {
            return this.map { true }
        }
    }

    open class FakeView : LockedView {
        open var _unlockButtonTaps = Observable.just(Unit)
        override val unlockButtonTaps: Observable<Unit>?
            get() = _unlockButtonTaps

        val activityResult = PublishSubject.create<Pair<Int, Int>>()
        override val onActivityResult: Observable<Pair<Int, Int>>
            get() = activityResult
    }

    class FakeLockedStore : LockedStore() {
        val onAuth = PublishSubject.create<FingerprintAuthAction>()
        override val onAuthentication: Observable<FingerprintAuthAction>
            get() = onAuth

        override val canLaunchAuthenticationOnForeground: Observable<Boolean> = PublishSubject.create()
    }

    @Mock
    private val fingerprintStore = Mockito.mock(FingerprintStore::class.java)
    private val lockedStore = FakeLockedStore()

    private val dispatcher = Dispatcher()
    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()
    val view = FakeView()

    lateinit var subject: FakeLockedPresenter

    @Before
    fun setUp() {
        PowerMockito.whenNew(FingerprintStore::class.java).withAnyArguments().thenReturn(fingerprintStore)

        subject = FakeLockedPresenter(view, dispatcher, fingerprintStore, lockedStore)
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Ignore("This test may be causing unknown failures in the build after upgrading to API 29; disabling for now")
    @Test
    fun `handle unlock confirmed true`() {
        val dispatchIterator = dispatcher.register.blockingIterable().iterator()
        view.activityResult.onNext(Pair(Constant.RequestCode.unlock, RESULT_OK))

        Assert.assertEquals(DataStoreAction.Unlock, dispatchIterator.next())
        val unlockingAction = dispatchIterator.next() as UnlockingAction
        Assert.assertFalse(unlockingAction.currently)
    }

    @Ignore("This test may be causing unknown failures in the build after upgrading to API 29; disabling for now")
    @Test
    fun `handle unlock confirmed false`() {
        val dispatchIterator = dispatcher.register.blockingIterable().iterator()
        view.activityResult.onNext(Pair(111, RESULT_CANCELED))

        val action = dispatchIterator.next() as AutofillAction
        Assert.assertEquals(AutofillAction.Cancel, action)

        val unlockingAction = dispatchIterator.next() as UnlockingAction
        Assert.assertFalse(unlockingAction.currently)
    }
}

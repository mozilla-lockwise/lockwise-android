/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Dispatcher
import org.junit.Before
import org.junit.Test

class LockedStoreTest {
    class FakeLifecycleStore : LifecycleStore() {
        override val lifecycleEvents: Observable<LifecycleAction> = PublishSubject.create()
    }

    private val dispatcher = Dispatcher()
    private val lifecycleStore = FakeLifecycleStore()
    private val authenticationObserver = TestObserver.create<FingerprintAuthAction>()
    private val authOnForegroundObserver = TestObserver.create<Boolean>()

    val subject = LockedStore(dispatcher, lifecycleStore)

    @Before
    fun setUp() {
        subject.onAuthentication
            .subscribe(authenticationObserver)

        subject.canLaunchAuthenticationOnForeground
            .subscribe(authOnForegroundObserver)
    }

    @Test
    fun `authentication events`() {
        val authAction = FingerprintAuthAction.OnSuccess
        dispatcher.dispatch(authAction)

        authenticationObserver.assertValue(authAction)
    }

    @Test
    fun `default auth on foreground`() {
        authOnForegroundObserver.assertValue(true)
    }

    @Test
    fun `getting datastore lock actions`() {
        dispatcher.dispatch(DataStoreAction.Lock)

        authOnForegroundObserver.assertLastValue(false)
    }

    @Test
    fun `getting background actions`() {
        dispatcher.dispatch(DataStoreAction.Lock)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)

        authOnForegroundObserver.assertLastValue(true)
    }

    @Test
    fun `getting unlocking and background lock actions`() {
        dispatcher.dispatch(UnlockingAction(true))
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)

        authOnForegroundObserver.assertLastValue(false)
    }

    @Test
    fun `getting true unlocking actions`() {
        dispatcher.dispatch(UnlockingAction(true))

        authOnForegroundObserver.assertLastValue(false)
    }

    @Test
    fun `getting false unlocking actions`() {
        dispatcher.dispatch(UnlockingAction(false))

        authOnForegroundObserver.assertLastValue(true)
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.store.DataStore.State
import mozilla.lockbox.support.AutoLockSupport
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class DataStoreTest : DisposingTest() {
    class FakeLifecycleStore : LifecycleStore() {
        override val lifecycleEvents: Observable<LifecycleAction> = PublishSubject.create()
    }

    class FakeAutolockSupport : AutoLockSupport() {
        var shouldLockStub: Boolean = false
        override val shouldLock
            get() = shouldLockStub

        override fun storeNextAutoLockTime() {
        }

        override fun backdateNextLockTime() {
        }

        override fun forwardDateNextLockTime() {
        }
    }

    private val support = MockDataStoreSupport()
    private val dispatcher = Dispatcher()
    private val autoLockSupport = spy(FakeAutolockSupport())
    private val lifecycleStore = FakeLifecycleStore()
    private val subject = DataStore(dispatcher, support, autoLockSupport, lifecycleStore)

    @Test
    fun testLockUnlock() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)
        clearInvocations(support.storage)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)
        verify(support.storage).sync(support.syncConfig!!)
        verify(support.storage, times(3)).list()

        dispatcher.dispatch(DataStoreAction.Lock)
        Assert.assertEquals(State.Locked, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)
    }

    @Test
    fun testTouch() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        clearInvocations(support.storage)

        val id = "lkjhkj"
        dispatcher.dispatch(DataStoreAction.Touch(id))
        listIterator.next()

        verify(support.storage).touch(id)
        verify(support.storage, atLeastOnce()).list()
    }

    @Test
    fun testResetUnpreparedState() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        dispatcher.dispatch(DataStoreAction.Reset)
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Mockito.verifyNoMoreInteractions(support.storage)
    }

    @Test
    fun testReset() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Reset)
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)

        verify(support.storage).wipeLocal()
    }

    @Test
    fun testUserReset() {
//        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(10, listIterator.next().size)
        clearInvocations(support.storage)

        dispatcher.dispatch(LifecycleAction.UserReset)

        // leaving this for later
//        stateIterator.next()
//        Assert.assertEquals(0, listIterator.next().size)
    }

    @Test
    fun testResetSupport() {
        val stateIterator = this.subject.state.blockingIterable().iterator()

        val newSupport = MockDataStoreSupport()
        Assert.assertNotSame("Support should be not the new one", newSupport, this.subject.support)

        this.subject.resetSupport(newSupport)
        Assert.assertSame("Support should be the new one", newSupport, this.subject.support)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())

        this.subject.resetSupport(MockDataStoreSupport())
        verify(newSupport.asyncStorage).wipeLocal()
    }

    @Test
    fun testUpdateCredentials() {
        val tokenServerURL = "www.mozilla.org"
        val kid = "dfsdfsfads"
        val k = "lololololol"
        val accessToken = "jlk;sfdkljdfsljk"
        val scope = "asdf1234"

        val syncCredentials = FixedSyncCredentials(
            accessToken = AccessTokenInfo(scope, accessToken, null, 0L),
            tokenServerURL = tokenServerURL,
            kid = kid,
            syncKey = k,
            isNew = false
        )
        val support = syncCredentials.support
        dispatcher.dispatch(
            DataStoreAction.UpdateCredentials(
                syncCredentials
            )
        )

        val expectedSyncUnlockInfo = SyncUnlockInfo(
            kid,
            accessToken,
            k,
            tokenServerURL
        )

        Assert.assertEquals(expectedSyncUnlockInfo.kid, support.syncConfig!!.kid)
        Assert.assertEquals(expectedSyncUnlockInfo.fxaAccessToken, support.syncConfig!!.fxaAccessToken)
        Assert.assertEquals(expectedSyncUnlockInfo.syncKey, support.syncConfig!!.syncKey)
        Assert.assertEquals(expectedSyncUnlockInfo.tokenserverURL, support.syncConfig!!.tokenserverURL)
    }

    @Test
    fun testSync() {
        val syncIterator = this.subject.syncState.blockingIterable().iterator()

        dispatcher.dispatch(DataStoreAction.Sync)
        Assert.assertEquals(DataStore.SyncState.Syncing, syncIterator.next())
        Assert.assertEquals(DataStore.SyncState.NotSyncing, syncIterator.next())
    }

    @Test
    fun testGet() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        val serverPassword = listIterator.next()[4]

        val serverPasswordIterator = this.subject.get(serverPassword.id).blockingIterable().iterator()

        Assert.assertEquals(serverPassword.asOptional(), serverPasswordIterator.next())
    }

    @Test
    fun testLockWhenLocked() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Lock)
        Assert.assertEquals(State.Locked, stateIterator.next())
    }

    @Test
    fun `receiving background actions when unlocked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(autoLockSupport).forwardDateNextLockTime()

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        verify(autoLockSupport).storeNextAutoLockTime()
    }

    @Test
    fun `receiving background actions when locked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(autoLockSupport).forwardDateNextLockTime()

        dispatcher.dispatch(DataStoreAction.Lock)
        verify(autoLockSupport).backdateNextLockTime()
        Assert.assertEquals(State.Locked, stateIterator.next())

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        verify(autoLockSupport, never()).storeNextAutoLockTime()
    }

    @Test
    fun `receiving foreground actions when should lock`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        autoLockSupport.shouldLockStub = true

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Foreground)

        Assert.assertEquals(State.Locked, stateIterator.next())
    }

    @Test
    fun `receiving foreground actions when should not lock`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        autoLockSupport.shouldLockStub = false
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Foreground)
    }

    @Test
    fun `receiving autofill end actions when unlocked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(autoLockSupport).forwardDateNextLockTime()

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillEnd)
        verify(autoLockSupport).storeNextAutoLockTime()
    }

    @Ignore("Todo: this test is failing.")
    @Test
    fun `receiving autofill end actions when locked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(autoLockSupport).forwardDateNextLockTime()

        dispatcher.dispatch(DataStoreAction.Lock)
        verify(autoLockSupport).backdateNextLockTime()

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillEnd)
        verify(autoLockSupport, never()).storeNextAutoLockTime()
    }

    @Test
    fun `receiving autofill start actions when should lock`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        autoLockSupport.shouldLockStub = true

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillStart)

        Assert.assertEquals(State.Locked, stateIterator.next())
    }

    @Test
    fun `receiving autofill start actions when should not lock`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        autoLockSupport.shouldLockStub = false
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillStart)
    }
}
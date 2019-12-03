/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.extensions.matches
import mozilla.lockbox.extensions.filter
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.model.FixedSyncCredentials
import mozilla.lockbox.store.DataStore.State
import mozilla.lockbox.support.TimingSupport
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import java.util.concurrent.atomic.AtomicBoolean
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@ExperimentalCoroutinesApi
class DataStoreTest : DisposingTest() {
    class FakeLifecycleStore : LifecycleStore() {
        override val lifecycleEvents: Observable<LifecycleAction> = PublishSubject.create()
    }

    @Mock
    val timingSupport = PowerMockito.mock(TimingSupport::class.java)!!

    private val support = MockDataStoreSupport()
    private val dispatcher = Dispatcher()
    private val lifecycleStore = FakeLifecycleStore()
    private lateinit var subject: DataStore

    val dispatcherObserver = TestObserver.create<Action>()!!

    @Before
    fun setUp() {
        PowerMockito.whenNew(TimingSupport::class.java).withAnyArguments().thenReturn(timingSupport)
        dispatcher.register.subscribe(dispatcherObserver)

        `when`(timingSupport.currentTimeMillis).thenReturn(System.currentTimeMillis())

        subject = DataStore(dispatcher, support, timingSupport, lifecycleStore)
    }

    @Test
    fun `update item details`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)
        clearInvocations(support.storage)
        whenCalled(timingSupport.shouldSync).thenReturn(false)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)

        val item = listIterator.next()[0]
        val newHostname = "https://ilovecats.com"

        val originalItem = ServerPassword(
            id = item.id,
            hostname = newHostname,
            username = item.username,
            password = item.password,
            httpRealm = item.httpRealm,
            formSubmitURL = item.formSubmitURL
        )

        val updatedItem = originalItem.copy(password = "new password")

        dispatcher.dispatch(DataStoreAction.UpdateItemDetail(originalItem, updatedItem))

        // check if the list is updated
        dispatcherObserver.assertValueAt(1, DataStoreAction.ListUpdate)
    }

    @Test
    fun `test fixupMutationMetadata`() {
        val original = ServerPassword(id = "id", hostname = "hostname.com", username = "username", password = "password")
        val change1 = original.copy(password = "newpassword")
        assertEquals(0, change1.timePasswordChanged)

        val fixup1 = subject.fixupMutationMetadata(original, change1)
        assertNotEquals(change1.timePasswordChanged, fixup1.timePasswordChanged)

        val change2 = original.copy(username = "newuser")
        val fixup2 = subject.fixupMutationMetadata(original, change2)
        assertEquals(change2.timePasswordChanged, fixup2.timePasswordChanged)
    }

    @Test
    fun `test issue 867 sync crash`() {
        support.syncConfig = null
        Assert.assertNull(support.syncConfig)
        subject.sync()
        Assert.assertNull(support.syncConfig)
        Assert.assertEquals(subject.syncStateSubject.value, DataStore.SyncState.NotSyncing)
    }

    fun testAddNewEntry() {
        // set up unlocked store
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        clearInvocations(support.storage)

        // create new entry and add
        val newEntry = ServerPassword(
            id = "",
            hostname = "cats.com",
            username = "feline",
            password = "iLUVkatz",
            formSubmitURL = "cats.com"
        )
        dispatcher.dispatch(DataStoreAction.AutofillCapture(newEntry))

        verify(support.storage).add(newEntry)
    }

    @Test
    fun testLockUnlock() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)
        clearInvocations(support.storage)
        whenCalled(timingSupport.shouldSync).thenReturn(true)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)
        verify(support.storage).sync(support.syncConfig!!)
        verify(support.storage, times(3)).list()
        verify(timingSupport).storeNextSyncTime()

        dispatcher.dispatch(DataStoreAction.Lock)
        Assert.assertEquals(State.Locked, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)
    }

    @Test
    fun testSyncIfRequired_dispatchesSyncAction() {
        whenCalled(timingSupport.shouldSync).thenReturn(true)
        val isSyncing = AtomicBoolean(false)
        val sub = dispatcher.register
            .filterByType(DataStoreAction::class.java)
            .subscribe {
                isSyncing.set(true)
            }

        subject.syncIfRequired()

        Assert.assertTrue(isSyncing.get())
        sub.dispose()
    }

    @Test
    fun testLockUnlock_shouldNotSync() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)
        clearInvocations(support.storage)
        whenCalled(timingSupport.shouldSync).thenReturn(false)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        Assert.assertEquals(10, listIterator.next().size)
        verify(support.storage, times(2)).list()
        verify(timingSupport, never()).storeNextSyncTime()

        dispatcher.dispatch(DataStoreAction.Lock)
        Assert.assertEquals(State.Locked, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)
    }

    @Test
    @Ignore("Flaky. Needs to be fixed.")
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
            DataStoreAction.UpdateSyncCredentials(
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
        Assert.assertEquals(
            expectedSyncUnlockInfo.fxaAccessToken,
            support.syncConfig!!.fxaAccessToken
        )
        Assert.assertEquals(expectedSyncUnlockInfo.syncKey, support.syncConfig!!.syncKey)
        Assert.assertEquals(
            expectedSyncUnlockInfo.tokenserverURL,
            support.syncConfig!!.tokenserverURL
        )
    }

    @Test
    fun testSync() {
        val syncIterator = this.subject.syncState.blockingIterable().iterator()
        Assert.assertEquals(DataStore.SyncState.NotSyncing, syncIterator.next())

        dispatcher.dispatch(DataStoreAction.Sync)
        Assert.assertEquals(DataStore.SyncState.Syncing, syncIterator.next())
    }

    @Test
    fun testGet() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        val serverPassword = listIterator.next()[4]

        val serverPasswordIterator =
            this.subject.get(serverPassword.id).blockingIterable().iterator()

        Assert.assertEquals(serverPassword.asOptional(), serverPasswordIterator.next())
    }

    @Test fun `find matching credentials`() {
        val item = ServerPassword(
            id = "id1",
            hostname = "example1.com",
            username = "user1",
            password = "",
            formSubmitURL = "form1",
            httpRealm = "realm1"
        )

        assertTrue(item.matches(hostname = "example1.com"))
        assertFalse(item.matches(hostname = "nonmatching.com"))
        assertTrue(item.matches(hostname = "example1.com", formSubmitURL = "form1"))
        assertFalse(item.matches(hostname = "example1.com", formSubmitURL = "nonmatching"))
        assertTrue(item.matches(hostname = "example1.com", formSubmitURL = "form1", httpRealm = "realm1"))
        assertFalse(item.matches(hostname = "example1.com", formSubmitURL = "form1", httpRealm = "nonmatching"))

        val list = listOf(
            ServerPassword(
                id = "id1",
                hostname = "example1.com",
                username = "user1",
                password = "",
                formSubmitURL = "form1"
            ),
            ServerPassword(
                id = "id2",
                hostname = "example1.com",
                username = "user2",
                password = "",
                formSubmitURL = "form2"
            ),
            ServerPassword(
                id = "id3",
                hostname = "example2.com",
                username = "user3",
                password = "",
                httpRealm = "realm1"
            ),
            ServerPassword(
                id = "id4",
                hostname = "example2.com",
                username = "user4",
                password = "",
                httpRealm = "realm2"
            )
        )

        assertEquals(2, list.filter(hostname = "example1.com").size)
        assertEquals(1, list.filter(hostname = "example1.com", formSubmitURL = "form1").size)

        assertEquals(2, list.filter(hostname = "example2.com").size)
        assertEquals(1, list.filter(hostname = "example2.com", httpRealm = "realm1").size)
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
    @Ignore("Flaky. Needs to be fixed.")
    fun `receiving background actions when unlocked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(timingSupport).forwardDateNextLockTime()

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Background)
        verify(timingSupport).storeNextAutoLockTime()
        verify(support.storage).ensureLocked()
    }

    @Test
    @Ignore("Flaky. Needs to be fixed.")
    fun `receiving background actions when locked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(timingSupport).forwardDateNextLockTime()

        dispatcher.dispatch(DataStoreAction.Lock)
        verify(timingSupport).backdateNextLockTime()
        Assert.assertEquals(State.Locked, stateIterator.next())
        clearInvocations(support.storage)

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillEnd)
        verify(timingSupport, never()).storeNextAutoLockTime()
        verify(support.storage).ensureLocked()
    }

    @Test
    fun `receiving foreground actions when should lock`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        whenCalled(timingSupport.shouldLock).thenReturn(true)

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
        clearInvocations(support.storage)
        whenCalled(timingSupport.shouldLock).thenReturn(false)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.Foreground)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(support.storage).ensureUnlocked(support.encryptionKey)
    }

    @Test
    fun `receiving autofill end actions when unlocked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(timingSupport).forwardDateNextLockTime()

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillEnd)
        verify(timingSupport).storeNextAutoLockTime()
    }

    @Test
    @Ignore("Flaky. Needs to be fixed.")
    fun `receiving autofill end actions when locked`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        verify(timingSupport).forwardDateNextLockTime()

        dispatcher.dispatch(DataStoreAction.Lock)
        verify(timingSupport).backdateNextLockTime()

        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillEnd)
        verify(timingSupport, never()).storeNextAutoLockTime()
    }

    @Test
    fun `receiving autofill start actions when should lock`() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        whenCalled(timingSupport.shouldLock).thenReturn(true)

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
        whenCalled(timingSupport.shouldLock).thenReturn(false)
        (lifecycleStore.lifecycleEvents as Subject).onNext(LifecycleAction.AutofillStart)
    }
}

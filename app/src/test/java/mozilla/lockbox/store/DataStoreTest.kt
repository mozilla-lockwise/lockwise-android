/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.fxa.OAuthScopedKey
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.model.SyncCredentials
import mozilla.lockbox.store.DataStore.State
import mozilla.lockbox.support.FxAOauthInfo
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verify
import java.lang.Thread.sleep

@ExperimentalCoroutinesApi
class DataStoreTest : DisposingTest() {
    private val support = MockDataStoreSupport()

    private lateinit var dispatcher: Dispatcher
    private lateinit var subject: DataStore

    val stateObserver = createTestObserver<State>()
    val listObserver = createTestObserver<List<ServerPassword>>()

    @Before
    fun setUp() {
        // TODO: mock backend for testing ...
        val dispatcher = Dispatcher()
        this.dispatcher = dispatcher
        subject = DataStore(dispatcher, support)

        subject.state.subscribe(stateObserver)
        subject.list.subscribe(listObserver)
    }

    @Test
    fun testInitialState() {
        Assert.assertSame(dispatcher, subject.dispatcher)
        subject.list.subscribe {
            Assert.assertTrue(it.isEmpty())
        }.addTo(disposer)
        subject.state.subscribe {
            Assert.assertEquals(State.Unprepared, it)
        }.addTo(disposer)
    }

    @Test
    fun testLockUnlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        sleep(100)

        dispatcher.dispatch(DataStoreAction.Lock)
        sleep(100)

        stateObserver.apply {
            // TODO: figure out why the initialized state isn't here?
            assertValues(State.Unprepared, State.Unlocking, State.Unlocked, State.Locked)
        }
        listObserver.apply {
            val results = values()
            Assert.assertEquals(3, results.size)
            Assert.assertEquals(0, results[0].size)
            Assert.assertEquals(10, results[1].size)
            Assert.assertEquals(0, results[2].size)
        }
    }

    @Test
    fun testTouch() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        sleep(100)
        clearInvocations(support.storage)

        val id = "lkjhkj"
        dispatcher.dispatch(DataStoreAction.Touch(id))

        verify(support.storage).touch(id)
        sleep(100)
        verify(support.storage).list()
    }

    @Test
    fun testReset() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        sleep(100)

        dispatcher.dispatch(DataStoreAction.Reset)
        sleep(100)

        verify(support.storage).reset()

        listObserver.assertLastValue(emptyList())
        stateObserver.assertLastValue(DataStore.State.Unprepared)
    }

    @Test
    fun testUserReset() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        sleep(100)

        dispatcher.dispatch(LifecycleAction.UserReset)
        sleep(100)

        verify(support.storage).reset()

        listObserver.assertLastValue(emptyList())
        stateObserver.assertLastValue(DataStore.State.Unprepared)
    }

    @Test
    fun testResetSupport() {
        val newSupport = MockDataStoreSupport()
        Assert.assertNotSame("Support should be not the new one", newSupport, subject.support)

        stateObserver.assertLastValue(State.Unprepared)
        subject.resetSupport(newSupport)
        Assert.assertSame("Support should be the new one", newSupport, subject.support)

        stateObserver.assertLastValue(State.Unprepared)

        dispatcher.dispatch(DataStoreAction.Unlock)
        sleep(100)

        stateObserver.assertLastValue(State.Unlocked)
        subject.resetSupport(MockDataStoreSupport())
        verify(newSupport.storage).reset()
    }

    @Test
    fun testUpdateCredentials() {
        val scope = "oldsync"
        val tokenServerURL = "www.mozilla.org"
        val kid = "dfsdfsfads"
        val k = "lololololol"
        val scopedKey = OAuthScopedKey(kid, k)
        val accessToken = "jlk;sfdkljdfsljk"
        val oauthInfo = object : FxAOauthInfo {
            override val accessToken: String = accessToken
            override val keys: Map<String, OAuthScopedKey>? = mapOf(Pair(scope, scopedKey))
            override val scopes: List<String> = listOf(scope)
        }
        val syncCredentials = SyncCredentials(
            oauthInfo,
            tokenServerURL,
            scope
        )
        dispatcher.dispatch(DataStoreAction.UpdateCredentials(
            syncCredentials
        ))

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
        val syncStateObserver = createTestObserver<DataStore.SyncState>()

        subject.syncState.subscribe(syncStateObserver)

        dispatcher.dispatch(DataStoreAction.Sync)
        sleep(100)

        syncStateObserver.apply {
            assertValueCount(2)
            assertValueAt(0, DataStore.SyncState.Syncing)
            assertValueAt(1, DataStore.SyncState.NotSyncing)
        }
    }
}

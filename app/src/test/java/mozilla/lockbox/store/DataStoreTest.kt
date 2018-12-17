/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.SyncUnlockInfo
import mozilla.components.service.fxa.OAuthScopedKey
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.mocks.MockDataStoreSupport
import mozilla.lockbox.model.FxASyncCredentials
import mozilla.lockbox.store.DataStore.State
import mozilla.lockbox.support.FxAOauthInfo
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class DataStoreTest : DisposingTest() {
    private val support = MockDataStoreSupport()
    private val dispatcher = Dispatcher()
    private val subject = DataStore(dispatcher, support)

    @Test
    fun testLockUnlock() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Lock)
        Assert.assertEquals(State.Locked, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)
    }

    @Test
    fun testTouch() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)
        clearInvocations(support.storage)

        val id = "lkjhkj"
        dispatcher.dispatch(DataStoreAction.Touch(id))
        listIterator.next()

        verify(support.storage).touch(id)
        verify(support.storage).list()
    }

    @Test
    fun testReset() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())
        Assert.assertEquals(10, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Reset)
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)

        verify(support.storage).reset()
    }

    @Test
    fun testUserReset() {
        val stateIterator = this.subject.state.blockingIterable().iterator()
        val listIterator = this.subject.list.blockingIterable().iterator()
        Assert.assertEquals(State.Unprepared, stateIterator.next())
        Assert.assertEquals(0, listIterator.next().size)

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(10, listIterator.next().size)

        dispatcher.dispatch(LifecycleAction.UserReset)

        // leaving this for later
//        stateIterator.next()
//        Assert.assertEquals(0, listIterator.next().size)

//        verify(support.storage).reset()
    }

    @Test
    fun testResetSupport() {
        val stateIterator = this.subject.state.blockingIterable().iterator()

        val newSupport = MockDataStoreSupport()
        Assert.assertNotSame("Support should be not the new one", newSupport, this.subject.support)

        this.subject.resetSupport(newSupport)
        Assert.assertSame("Support should be the new one", newSupport, this.subject.support)

        Assert.assertEquals(State.Unprepared, stateIterator.next())

        dispatcher.dispatch(DataStoreAction.Unlock)
        Assert.assertEquals(State.Unlocked, stateIterator.next())

        this.subject.resetSupport(MockDataStoreSupport())
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
        val syncCredentials = FxASyncCredentials(
            oauthInfo,
            tokenServerURL,
            scope,
            false
        )
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
}

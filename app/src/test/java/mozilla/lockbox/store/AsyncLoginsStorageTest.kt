/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.store

import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import mozilla.appservices.logins.DatabaseLoginsStorage
import mozilla.appservices.logins.LoginsStorage
import mozilla.appservices.logins.MemoryLoginsStorage
import mozilla.components.concept.storage.SyncError
import mozilla.components.concept.storage.SyncOk
import mozilla.components.concept.storage.SyncStatus
import mozilla.components.concept.storage.SyncableStore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import java.io.File

import mozilla.components.service.sync.logins.AsyncLoginsStorageAdapter
import mozilla.components.service.sync.logins.ServerPassword

import mozilla.appservices.LockboxMegazord

@RunWith(RobolectricTestRunner::class)
class AsyncLoginsStorageTest {
    private val testMainScope = CoroutineScope(newSingleThreadContext("Test"))

    companion object {
        @BeforeClass @JvmStatic fun `configure native library names`() {
            // We must configure the library path names, 'cuz Robolectric
            // does not invoke `Application.onCreate`.
            LockboxMegazord.init()
        }
    }

    @Test
    fun `can do some stuff`() {

        AsyncLoginsStorageAdapter.forDatabase(File("/tmp/path").canonicalPath).use { storage ->

        runBlocking(testMainScope.coroutineContext) {

                    storage.unlock("").await()


                    val list1 = storage.list().await()
                assertEquals(0, list1.size)

        storage.add(ServerPassword(
                id = "aaaaaaaaaaaa",
                hostname = "https://www.example.com",
                httpRealm = "Something",
                username = "Foobar2000",
                password = "hunter2",
                usernameField = "users_name",
                passwordField = "users_password"
                                 )).await()

                    val list2 = storage.list().await()
                assertEquals(1, list2.size)

                    storage.wipe().await()

                    val list3 = storage.list().await()
                assertEquals(0, list3.size)

            storage.close()
                }
            }
    }
}

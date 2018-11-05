/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import mozilla.lockbox.model.ItemViewModel
import org.junit.Assert
import org.junit.Test
import org.mozilla.sync15.logins.ServerPassword
import java.util.Date

class ServerPasswordExtensionTest {
    @Test
    fun toViewModel() {
        val guid = "afdsfdsa"
        val username = "cats@cats.com"
        val serverPassword = ServerPassword(
                guid,
                hostname = "www.mozilla.org",
                username = username,
                password = "woof")

        val expectedItemViewModel = ItemViewModel("mozilla.org", username, guid, Date().time)

        Assert.assertEquals(serverPassword.toViewModel(), expectedItemViewModel)
    }
}
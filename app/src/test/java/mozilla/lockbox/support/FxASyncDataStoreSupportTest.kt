/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import org.junit.Assert
import org.junit.Test

class FxASyncDataStoreSupportTest {
    @Test
    fun `test encryption key generation`() {
        val pattern = Regex("[0-9a-f]+", RegexOption.IGNORE_CASE)

        var keyStr = generateEncryptionKey(128)
        Assert.assertEquals(32, keyStr.length)
        Assert.assertTrue(pattern.matches(keyStr))

        var oldKeyStr = keyStr
        keyStr = generateEncryptionKey(256)
        Assert.assertEquals(64, keyStr.length)
        Assert.assertTrue(pattern.matches(keyStr))
        Assert.assertFalse(keyStr == oldKeyStr)

        oldKeyStr = keyStr
        keyStr = generateEncryptionKey(256)
        Assert.assertEquals(64, keyStr.length)
        Assert.assertTrue(pattern.matches(keyStr))
        Assert.assertFalse(keyStr == oldKeyStr)
    }
}
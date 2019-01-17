/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import org.junit.Assert
import org.junit.Test

class HostnameSupportTest {
    @Test
    fun `converts a package name`() {
        var support = HostnameSupport.fromPackageName("com.example.android")
        Assert.assertEquals("android.example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        support = HostnameSupport.fromPackageName("uk.co.example.android")
        Assert.assertEquals("android.example.co.uk", support.fullName)
        Assert.assertEquals("example.co.uk", support.topName)

        support = HostnameSupport.fromPackageName("com.blogspot.something.android")
        Assert.assertEquals("android.something.blogspot.com", support.fullName)
        Assert.assertEquals("something.blogspot.com", support.topName)

        support = HostnameSupport.fromPackageName("example.example.android")
        Assert.assertEquals("android.example.example", support.fullName)
        Assert.assertEquals("example.example", support.topName)

        support = HostnameSupport.fromPackageName("")
        Assert.assertEquals("", support.fullName)
        Assert.assertEquals("", support.topName)
    }
}
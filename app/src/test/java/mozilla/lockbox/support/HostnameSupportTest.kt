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
    fun `test creates from a domain name`() {
        // tld + 1
        var support = HostnameSupport.fromDomainName("example.com")
        Assert.assertEquals("example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        // tld + 2
        support = HostnameSupport.fromDomainName("www.example.com")
        Assert.assertEquals("www.example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        // tld + 3
        support = HostnameSupport.fromDomainName("us1.api.example.com")
        Assert.assertEquals("us1.api.example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        // registry suffix
        support = HostnameSupport.fromDomainName("www.example.co.uk")
        Assert.assertEquals("www.example.co.uk", support.fullName)
        Assert.assertEquals("example.co.uk", support.topName)

        // public suffix
        support = HostnameSupport.fromDomainName("www.something.blogspot.com")
        Assert.assertEquals("www.something.blogspot.com", support.fullName)
        Assert.assertEquals("something.blogspot.com", support.topName)

        // non-existent suffix
        support = HostnameSupport.fromDomainName("www.example.example")
        Assert.assertEquals("www.example.example", support.fullName)
        Assert.assertEquals("example.example", support.topName)

        //

        support = HostnameSupport.fromDomainName("")
        Assert.assertEquals("", support.fullName)
        Assert.assertEquals("", support.topName)
    }

    @Test
    fun `test creates from a URI string`() {
        var support = HostnameSupport.fromUri("https://example.com/")
        Assert.assertEquals("example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        support = HostnameSupport.fromUri("https://example.com:8888/")
        Assert.assertEquals("example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        support = HostnameSupport.fromUri("http://www.example.com/")
        Assert.assertEquals("www.example.com", support.fullName)
        Assert.assertEquals("example.com", support.topName)

        support = HostnameSupport.fromUri("https://www.example.co.uk/")
        Assert.assertEquals("www.example.co.uk", support.fullName)
        Assert.assertEquals("example.co.uk", support.topName)

        support = HostnameSupport.fromUri("https://www.something.blogspot.com/")
        Assert.assertEquals("www.something.blogspot.com", support.fullName)
        Assert.assertEquals("something.blogspot.com", support.topName)

        support = HostnameSupport.fromUri("http://www.example.example/")
        Assert.assertEquals("www.example.example", support.fullName)
        Assert.assertEquals("example.example", support.topName)

        support = HostnameSupport.fromUri("")
        Assert.assertEquals("", support.fullName)
        Assert.assertEquals("", support.topName)
    }

    @Test
    fun `test creates from a package name`() {
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

    @Test
    fun testMatches() {
        var credHost = HostnameSupport.fromDomainName("example.com")
        var formHost = HostnameSupport.fromDomainName("example.com")
        Assert.assertTrue(credHost.matches(formHost))
        Assert.assertTrue(formHost.matches(credHost))

        formHost = HostnameSupport.fromDomainName("www.example.com")
        Assert.assertTrue(credHost.matches(formHost))
        Assert.assertTrue(formHost.matches(credHost))

        formHost = HostnameSupport.fromDomainName("en.android.example.com")
        Assert.assertTrue(credHost.matches(formHost))
        Assert.assertTrue(formHost.matches(credHost))

        credHost = HostnameSupport.fromDomainName("app.example.com")
        Assert.assertTrue(credHost.matches(formHost))
        Assert.assertTrue(formHost.matches(credHost))

        formHost = HostnameSupport.fromDomainName("app.example.com.co")
        Assert.assertFalse(credHost.matches(formHost))
        Assert.assertFalse(formHost.matches(credHost))

        formHost = HostnameSupport.fromDomainName("")
        Assert.assertFalse(credHost.matches(formHost))
        Assert.assertFalse(formHost.matches(credHost))
    }
}
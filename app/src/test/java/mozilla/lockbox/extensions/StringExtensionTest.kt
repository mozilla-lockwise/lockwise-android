/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import org.junit.Assert
import org.junit.Test

class StringExtensionTest {
    @Test
    fun titleFromHostname() {
        Assert.assertEquals("bats.com", "www.bats.com".titleFromHostname())
        Assert.assertEquals("hey.http://.com", "https://hey.http://.com".titleFromHostname())
        Assert.assertEquals("maps.com", "http://www.maps.com".titleFromHostname())
        Assert.assertEquals("stuff.example", "http://www2.stuff.example".titleFromHostname())
        Assert.assertEquals("stuff.example", "http://www12345.stuff.example".titleFromHostname())
        Assert.assertEquals("www2api.stuff.example", "http://www2api.stuff.example".titleFromHostname())
        Assert.assertEquals("www-api.maps.com", "http://www-api.maps.com".titleFromHostname())
    }
}

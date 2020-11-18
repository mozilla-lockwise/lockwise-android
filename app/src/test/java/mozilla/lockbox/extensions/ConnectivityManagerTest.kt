/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.powermock.modules.junit4.PowerMockRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
class ConnectivityManagerTest {

    @Mock
    val connectivityManager: ConnectivityManager = mock(ConnectivityManager::class.java)

    @Test
    fun `connectManager is online works`() {

        val network = mock(Network::class.java)
        val networkCapabilities = mock(NetworkCapabilities::class.java)

        whenCalled(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenCalled(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true)
        whenCalled(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(true)

        assertTrue(connectivityManager.isOnline(network))
    }

    @Test
    fun `connectManager is online with unvalidated connection works`() {

        val network = mock(Network::class.java)
        val networkCapabilities = mock(NetworkCapabilities::class.java)

        whenCalled(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenCalled(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true)
        whenCalled(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(false)

        assertFalse(connectivityManager.isOnline(network))
    }

    @Test
    fun `connectManager is online with no connection works`() {

        val network = mock(Network::class.java)
        val networkCapabilities = mock(NetworkCapabilities::class.java)

        whenCalled(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenCalled(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(false)
        whenCalled(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(true)

        assertFalse(connectivityManager.isOnline(network))
    }
}
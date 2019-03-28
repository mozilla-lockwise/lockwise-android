package mozilla.lockbox.store

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.preference.PreferenceManager
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class)
class NetworkStoreTest : DisposingTest() {
    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    val connectivityManager: ConnectivityManager = Mockito.mock(ConnectivityManager::class.java)

    @Mock
    val networkInfo: NetworkInfo = Mockito.mock(NetworkInfo::class.java)

    val dispatcher = Dispatcher()
    val subject = NetworkStore(dispatcher)

    @Before
    fun setUp() {
        subject.connectivityManager = connectivityManager
        whenCalled(subject.connectivityManager.activeNetworkInfo)
            .thenReturn(networkInfo)
    }

    @Test
    fun checkConnectivity_returnFalse() {
        var expectedNetworkValue = false
        whenCalled(subject.isConnectedState).thenReturn(expectedNetworkValue)
        dispatcher.dispatch(NetworkAction.CheckConnectivity)

        assertEquals(expectedNetworkValue, subject.isConnectedState)
    }

    @Test
    fun checkConnectivity_returnTrue() {
        var expectedNetworkValue = true
        whenCalled(subject.isConnectedState).thenReturn(expectedNetworkValue)
        dispatcher.dispatch(NetworkAction.CheckConnectivity)

        assertEquals(expectedNetworkValue, subject.isConnectedState)
    }
}
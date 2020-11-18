package mozilla.lockbox.store

import android.content.Context
import androidx.preference.PreferenceManager
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(PreferenceManager::class)
class NetworkStoreTest : DisposingTest() {
    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    val dispatcher = Dispatcher()
    val subject = NetworkStore(dispatcher)

    @Test
    fun checkConnectivity_returnFalse() {
        var expectedNetworkValue = false
        subject.isConnectedState = expectedNetworkValue
        dispatcher.dispatch(NetworkAction.CheckConnectivity)

        assertEquals(expectedNetworkValue, subject.isConnectedState)
    }

    @Test
    fun checkConnectivity_returnTrue() {
        var expectedNetworkValue = true
        subject.isConnectedState = expectedNetworkValue
        dispatcher.dispatch(NetworkAction.CheckConnectivity)

        assertEquals(expectedNetworkValue, subject.isConnectedState)
    }
}
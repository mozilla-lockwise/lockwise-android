package mozilla.lockbox

import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenCalled
import org.mockito.Mockito.mock

@ExperimentalCoroutinesApi
class LockboxAutofillServiceTest {
    @Mock
    val fillRequest: FillRequest = mock(FillRequest::class.java)

    @Mock
    val fillContext: FillContext = mock(FillContext::class.java)

    class FakeDataStore : DataStore() {
        override val list: Observable<List<ServerPassword>> = PublishSubject.create()
    }

    private val dispatcher = Dispatcher()
    private val dataStore = FakeDataStore()

    val subject = LockboxAutofillService(dataStore, dispatcher)

    @Before
    fun setUp() {
        subject.onConnected()
    }

    @After
    fun tearDown() {
        subject.onDisconnected()
    }

    @Test
    fun fillRequestWithUsernameAndPassword() {
    }

    @Test
    fun fillRequestWithUsername() {
    }

    @Test
    fun fillRequestWithPassword() {
    }

    @Test
    fun fillRequestWithNeither() {
        val assistStructure = AssistStructure()

        whenCalled(fillContext.structure).thenReturn(assistStructure)
        stubFillContext()
    }

    private fun stubFillContext() {
        val fillContexts = arrayListOf(fillContext)
        whenCalled(fillRequest.fillContexts).thenReturn(fillContexts)
    }
}
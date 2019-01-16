package mozilla.lockbox

import android.app.assist.AssistStructure
import android.content.ComponentName
import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.view.View
import android.view.autofill.AutofillId
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled

@ExperimentalCoroutinesApi
class LockboxAutofillServiceTest {
    @Mock
    val fillRequest: FillRequest = mock(FillRequest::class.java)

    @Mock
    val fillContext: FillContext = mock(FillContext::class.java)

    @Mock
    val fillCallback: FillCallback = mock(FillCallback::class.java)

    @Mock
    val assistStructure: AssistStructure = mock(AssistStructure::class.java)

    @Mock
    val windowNode: AssistStructure.WindowNode = mock(AssistStructure.WindowNode::class.java)

    @Mock
    val rootViewNode = mock(AssistStructure.ViewNode::class.java)

    private val cancellationSignal = CancellationSignal()

    val twitterCredential = ServerPassword(
        "kjlfdsjlkf",
        "twitter.com",
        "cats@cats.com",
        "dawgzone"
    )

    class FakeDataStore : DataStore() {
        override val list: Observable<List<ServerPassword>> = PublishSubject.create()
    }

    private val dispatcher = Dispatcher()
    private val dataStore = FakeDataStore()

    val subject = LockboxAutofillService(dataStore, dispatcher)

    @Before
    fun setUp() {
        clearInvocations(fillRequest)
        clearInvocations(fillContext)
        clearInvocations(fillCallback)
        subject.onConnected()
    }

    @After
    fun tearDown() {
        subject.onDisconnected()
    }

    @Test
    fun fillRequestWithUsernameAndPasswordAutofillHints() {
//        val usernameId = mock(AutofillId::class.java)
//        val passwordId = mock(AutofillId::class.java)
//
//        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
//        whenCalled(usernameViewNode.autofillHints).thenReturn(arrayOf(View.AUTOFILL_HINT_USERNAME))
//        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
//        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)
//        whenCalled(passwordViewNode.autofillHints).thenReturn(arrayOf(View.AUTOFILL_HINT_PASSWORD))
//        whenCalled(usernameViewNode.autofillId).thenReturn(passwordId)
//
//        whenCalled(rootViewNode.childCount).thenReturn(2)
//        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameViewNode)
//        whenCalled(rootViewNode.getChildAt(1)).thenReturn(passwordViewNode)
//
//        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)
//
//        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
//        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)
//
//        stubFillContext(assistStructure)
//
//        subject.onFillRequest(fillRequest, cancellationSignal, fillCallback)
//
//        (dataStore.list as Subject).onNext(listOf(twitterCredential))
//
//        val callbackCaptor = ArgumentCaptor.forClass(FillResponse::class.java)
//        verify(fillCallback).onSuccess(callbackCaptor.capture())
//
//        val callback = callbackCaptor.value
//
//        Assert.assertEquals("", callback.toString())
    }

    @Test
    fun fillRequestWithUsername() {
    }

    @Test
    fun fillRequestWithPassword() {
    }

    @Test
    fun fillRequestWithNeither() {
    }

    private fun stubFillContext(assistStructure: AssistStructure) {
        val componentName = mock(ComponentName::class.java)
        whenCalled(componentName.packageName).thenReturn("com.twitter.android")
        whenCalled(assistStructure.activityComponent).thenReturn(componentName)
        whenCalled(fillContext.structure).thenReturn(assistStructure)
        val fillContexts = arrayListOf(fillContext)
        whenCalled(fillRequest.fillContexts).thenReturn(fillContexts)
    }
}
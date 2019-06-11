package mozilla.lockbox.presenter

import android.net.ConnectivityManager
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.support.Constant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(application = TestApplication::class)
class AppWebPageActionPresenterTest {

    class WebPageViewFake : WebPageView {

//        val retryButtonStub = PublishSubject.create<Unit>()
//        override val retryNetworkConnectionClicks: Observable<Unit>
//            get() = retryButtonStub

        var networkAvailable = PublishSubject.create<Boolean>()
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable.onNext(networkErrorVisibility)
        }

        var loadedUrl: String? = null
        override var webViewObserver: Consumer<String>? = null
        override fun loadURL(url: String) {
            loadedUrl = url
        }
    }

    private var view = WebPageViewFake()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()!!

    @Mock
    val networkStore = PowerMockito.mock(NetworkStore::class.java)!!

    private var isConnected: Observable<Boolean> = PublishSubject.create()
    var isConnectedObserver = TestObserver.create<Boolean>()

    @Mock
    private val connectivityManager = PowerMockito.mock(ConnectivityManager::class.java)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

        Mockito.`when`(networkStore.isConnected).thenReturn(isConnected)
        networkStore.connectivityManager = connectivityManager
        view.networkAvailable.subscribe(isConnectedObserver)
    }

    @Test
    fun `load faq url when menu item is selected`() {
        val expectedFaqUrl = Constant.Faq.uri

        val subject = AppWebPagePresenter(view, expectedFaqUrl, networkStore, dispatcher)
        subject.onViewReady()

        assertEquals(view.loadedUrl, expectedFaqUrl)
    }

    @Test
    fun `load send feedback url when menu item is selected`() {
        val expectedFeedbackUrl = Constant.SendFeedback.uri

        val subject = AppWebPagePresenter(view, expectedFeedbackUrl, networkStore, dispatcher)
        subject.onViewReady()

        assertEquals(view.loadedUrl, expectedFeedbackUrl)
    }

    @Test
    fun `network error visibility is correctly being set`() {
        val url = Constant.SendFeedback.uri

        val subject = AppWebPagePresenter(view, url, networkStore, dispatcher)
        subject.onViewReady()

        val value = view.networkAvailable
        value.onNext(true)

        isConnectedObserver.assertValue(true)
    }
}
package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert.assertEquals
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.support.Constant
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppWebPagePresenterTest {

    class WebPageViewFake : WebPageView {

        val retryButtonStub = PublishSubject.create<Unit>()
        override val retryNetworkConnectionClicks: Observable<Unit>
            get() = retryButtonStub

        var networkAvailable: Boolean = false
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable = networkErrorVisibility
        }

        open var loadedUrl: String? = null
        override var webViewObserver: Consumer<String>? = null
        override fun loadURL(url: String) {
            loadedUrl = url
        }
    }

    private var view = WebPageViewFake()
    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()!!
    private var networkStore = NetworkStore

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
    }

    @Test
    fun `load faq url when menu item is selected`() {
        val expectedFaqUrl = Constant.Faq.uri

        val subject = AppWebPagePresenter(view, expectedFaqUrl, networkStore.shared, dispatcher)
        subject.onViewReady()

        assertEquals(view.loadedUrl, expectedFaqUrl)
    }

    @Test
    fun `load send feedback url when menu item is selected`() {
        val expectedFeedbackUrl = Constant.SendFeedback.uri

        val subject = AppWebPagePresenter(view, expectedFeedbackUrl, networkStore.shared, dispatcher)
        subject.onViewReady()

        assertEquals(view.loadedUrl, expectedFeedbackUrl)
    }

    @Test
    fun `network connection is being checked on view ready`() {
        val subject = AppWebPagePresenter(view, Mockito.anyString(), networkStore.shared, dispatcher)
        subject.onViewReady()
        Assert.assertEquals(true, view.networkAvailable)
    }
}
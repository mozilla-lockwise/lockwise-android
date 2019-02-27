/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.isDebug
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import kotlin.jvm.internal.Reflection
import org.mockito.Mockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@PrepareForTest(AccountStore::class)
@Config(application = TestApplication::class)
class FxALoginPresenterTest : DisposingTest() {
    class FakeFxALoginView(
        compositeDisposable: CompositeDisposable
    ) : FxALoginView {

//        private val retryButtonStub = PublishSubject.create<Unit>()
//        override val retryNetworkConnectionClicks: Observable<Unit>
//            get() = retryButtonStub

        var networkAvailable = PublishSubject.create<Boolean>()
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable.onNext(networkErrorVisibility)
        }

        val webViewRedirectTo = PublishSubject.create<Uri>()
        val webViewOverride = PublishSubject.create<Boolean?>()
        var loadedURL: String? = null
        override var webViewRedirect: (url: Uri?) -> Boolean = { _ -> false }
        override fun loadURL(url: String) {
            loadedURL = url
        }

        override var skipFxAClicks: Observable<Unit> = PublishSubject.create<Unit>()

        init {
            webViewRedirectTo.subscribe {
                webViewOverride.onNext(webViewRedirect(it))
            }.addTo(compositeDisposable)
        }
    }

    class FakeFingerprintStore : FingerprintStore() {
        var fingerprintAvailableStub = false
        override val isFingerprintAuthAvailable: Boolean
            get() = fingerprintAvailableStub
    }


    class FakeSettingStore : SettingStore() {
        private val isAutofillAvailableStub: Boolean = true
        private val hasEnabledAutofillServicesStub: Boolean = true

        @Mock
        val autofillManagerStub = PowerMockito.mock(AutofillManager::class.java)

        override val autofillAvailable: Boolean
            get() = isAutofillAvailableStub

        override val isCurrentAutofillProvider: Boolean
            get() = hasEnabledAutofillServicesStub

        override var autofillManager: AutofillManager = autofillManagerStub

    }

    val view = FakeFxALoginView(disposer)
    val fingerprintStore = FakeFingerprintStore()

    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()

    val settingStore = FakeSettingStore()

    @Mock
    val accountStore = PowerMockito.mock(AccountStore::class.java)!!

    private val loginURLSubject = PublishSubject.create<String>()

    @Mock
    val networkStore = PowerMockito.mock(NetworkStore::class.java)!!

    private var isConnected: Observable<Boolean> = PublishSubject.create()
    var isConnectedObserver = TestObserver.create<Boolean>()

    @Mock
    private val connectivityManager = PowerMockito.mock(ConnectivityManager::class.java)

    lateinit var subject: FxALoginPresenter

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 26)

        whenCalled(networkStore.isConnected).thenReturn(isConnected)
        whenCalled(accountStore.loginURL).thenReturn(loginURLSubject)

        PowerMockito.whenNew(AccountStore::class.java).withAnyArguments().thenReturn(accountStore)

        dispatcher.register.subscribe(dispatcherObserver)

        networkStore.connectivityManager = connectivityManager
        view.networkAvailable.subscribe(isConnectedObserver)

        subject = FxALoginPresenter(view, dispatcher, networkStore, accountStore, fingerprintStore, settingStore)
        subject.onViewReady()
    }

    @Test
    fun `isRedirectURI is working as expected`() {
        var url: String? = null
        Assert.assertFalse(subject.isRedirectUri(url))
        url = "https://www.mozilla.org/"
        Assert.assertFalse(subject.isRedirectUri(url))
        url = Constant.FxA.redirectUri + "?moz_fake"
        Assert.assertTrue(subject.isRedirectUri(url))
    }

    @Test
    fun `onViewReady, when the accountStore pushes a new loginURL`() {
        val url = "www.mozilla.org"
        loginURLSubject.onNext(url)

        Assert.assertEquals(url, view.loadedURL)
    }

    @Test
    fun `onViewReady, when the webview redirects to a URL starting with the expected redirect and the device has fingerprints`() {
        fingerprintStore.fingerprintAvailableStub = true
        val url = Uri.parse(Constant.FxA.redirectUri + "?moz_fake")
        view.webViewRedirectTo.onNext(url)

        Assert.assertEquals(OnboardingStatusAction(true), dispatcherObserver.values()[0])
        val redirectAction = dispatcherObserver.values()[1] as AccountAction.OauthRedirect
        Assert.assertEquals(url.toString(), redirectAction.url)
        Assert.assertEquals(RouteAction.Onboarding.FingerprintAuth, dispatcherObserver.values()[2])
    }

    @Test
    fun `onViewReady, when the webview redirects to onboarding confirmation when autofill is not supported and the device has no fingerprints`() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 15)

        fingerprintStore.fingerprintAvailableStub = false
        val url = Uri.parse(Constant.FxA.redirectUri + "?moz_fake")
        view.webViewRedirectTo.onNext(url)

        Assert.assertEquals(OnboardingStatusAction(true), dispatcherObserver.values()[0])
        val redirectAction = dispatcherObserver.values()[1] as AccountAction.OauthRedirect
        Assert.assertEquals(url.toString(), redirectAction.url)
        Assert.assertEquals(RouteAction.Onboarding.Confirmation, dispatcherObserver.values()[2])
    }

    @Test
    fun `onViewReady, when the webview redirects to autofill onboarding screen when the device has no fingerprints and build number is supported for autofill`() {

        fingerprintStore.fingerprintAvailableStub = false
        val url = Uri.parse(Constant.FxA.redirectUri + "?moz_fake")
        view.webViewRedirectTo.onNext(url)

        Assert.assertEquals(OnboardingStatusAction(true), dispatcherObserver.values()[0])
        val redirectAction = dispatcherObserver.values()[1] as AccountAction.OauthRedirect
        Assert.assertEquals(url.toString(), redirectAction.url)
        Assert.assertEquals(RouteAction.Onboarding.Autofill, dispatcherObserver.values()[2])
    }

    @Test
    fun `onViewReady, when the webview redirects to a URL not starting with the expected redirect no dispatch happens`() {
        val url = Uri.parse("https://www.mozilla.org")
        view.webViewRedirectTo.onNext(url)

        dispatcherObserver.assertEmpty()
    }

    @Test
    fun `onViewReady, when the skipFXA button is tapped`() {
        if (isDebug()) {
            (view.skipFxAClicks as PublishSubject).onNext(Unit)

            dispatcherObserver.assertValueAt(0, OnboardingStatusAction(true))
            dispatcherObserver.assertValueAt(1, LifecycleAction.UseTestData)
        }
    }

    @Test
    fun `network error visibility is correctly being set`() {
        val value = view.networkAvailable
        value.onNext(true)

        isConnectedObserver.assertValue(true)
    }
}

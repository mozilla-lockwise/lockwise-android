/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.support.Constant
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(PowerMockRunner::class)
@PrepareForTest(AccountStore::class)
class FxALoginPresenterTest {
    class FakeFxALoginView : FxALoginView {
        val webViewRedirects = PublishSubject.create<String>()
        var loadedURL: String? = null

        override var webViewObserver: Consumer<String?>?
            get() = TODO("not implemented")
            set(value) {
                this.webViewRedirects.subscribe(value)
            }

        override fun loadURL(url: String) {
            loadedURL = url
        }
    }

    val view = FakeFxALoginView()

    @Mock
    val accountStore = PowerMockito.mock(AccountStore::class.java)

    private val loginURLSubject = PublishSubject.create<String>()
    private val dispatcherObserver = TestObserver.create<Action>()

    lateinit var subject: FxALoginPresenter

    @Before
    fun setUp() {
        whenCalled(accountStore.loginURL).thenReturn(loginURLSubject)

        PowerMockito.whenNew(AccountStore::class.java).withAnyArguments().thenReturn(accountStore)
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        subject = FxALoginPresenter(view, accountStore = accountStore)
        subject.onViewReady()
    }

    @Test
    fun onViewReady_loginURL() {
        val url = "www.mozilla.org"
        (accountStore.loginURL as PublishSubject).onNext(url)

        Assert.assertEquals(url, view.loadedURL)
    }

    @Test
    fun onViewReady_gettingURL_matchingRedirect() {
        val url = Constant.FxA.redirectUri + "/moz_fake"
        view.webViewRedirects.onNext(url)

        dispatcherObserver.assertValue(AccountAction.OauthRedirect(url))
    }

    @Test
    fun onViewReady_gettingURL_notMatchingRedirect() {
        val url = "www.mozilla.org"
        view.webViewRedirects.onNext(url)

        dispatcherObserver.assertEmpty()
    }
}

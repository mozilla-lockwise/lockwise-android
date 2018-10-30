/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.AccountAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.support.Constant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.robolectric.RobolectricTestRunner
import org.mockito.Mockito.`when` as whenCalled

@RunWith(RobolectricTestRunner::class)
class FxALoginPresenterTest {
    @Mock
    val view = Mockito.mock(FxALoginView::class.java)

    @Mock
    val accountStore = Mockito.mock(AccountStore::class.java)

    val dispatcherObserver = TestObserver.create<Action>()
    val urlObservable = PublishSubject.create<String>()

    lateinit var subject: FxALoginPresenter

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        whenCalled(accountStore.loginURL).thenReturn(urlObservable)

        subject = FxALoginPresenter(view, accountStore = accountStore)
    }

    @Test
    fun onViewReady_loginURL() {
        subject.onViewReady()
        val url = "www.mozilla.org"
        urlObservable.onNext(url)

        verify(view).loadURL(url)
    }

    @Test
    fun onViewReady_gettingURL_matchingRedirect() {
        subject.onViewReady()
        val url = Constant.FxA.redirectUri + "/moz_fake"
        view.webViewObserver?.accept(url)

        dispatcherObserver.assertValue(AccountAction.OauthRedirect(url))
    }

    @Test
    fun onViewReady_gettingURL_notMatchingRedirect() {
        subject.onViewReady()
        val url = "www.mozilla.org"
        view.webViewObserver?.accept(url)

        dispatcherObserver.assertEmpty()
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.NetworkAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.spy
import org.mockito.Mockito.verifyZeroInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemDetailPresenterTest {
    class FakeView : ItemDetailView {
        private val retryButtonStub = PublishSubject.create<Unit>()
        override val retryNetworkConnectionClicks: Observable<Unit>
            get() = retryButtonStub

        var networkAvailable: Boolean = false
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable = networkErrorVisibility
        }

        var item: ItemDetailViewModel? = null

        var toastNotificationArgument: Int? = null

        override val usernameCopyClicks = PublishSubject.create<Unit>()
        override val passwordCopyClicks = PublishSubject.create<Unit>()
        override val togglePasswordClicks = PublishSubject.create<Unit>()
        override val hostnameClicks = PublishSubject.create<Unit>()

        override var isPasswordVisible: Boolean = false

        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
        }

        override fun showToastNotification(@StringRes strId: Int) {
            toastNotificationArgument = strId
        }
    }

    class FakeDataStore : DataStore() {
        var idArg: String? = null
        val getStub = PublishSubject.create<Optional<ServerPassword>>()

        override fun get(id: String): Observable<Optional<ServerPassword>> {
            idArg = id
            return getStub
        }
    }

    val view = spy(FakeView())
    val dataStore = FakeDataStore()
    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()

    private val networkStore = NetworkStore

    private val fakeCredential: ServerPassword by lazy {
        ServerPassword(
            "id0",
            "https://www.mozilla.org",
            "dogs@dogs.com",
            "woof",
            timesUsed = 0,
            timeCreated = 0L,
            timeLastUsed = 0L,
            timePasswordChanged = 0L
        )
    }

    val subject = ItemDetailPresenter(view, fakeCredential.id, dispatcher, networkStore.shared, dataStore)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

        subject.onViewReady()
        dataStore.getStub.onNext(fakeCredential.asOptional())
    }

    @Test
    fun `sends a detail view model to view`() {
        Assert.assertEquals(fakeCredential.id, dataStore.idArg)

        // test the results that the view gets.
        val obs = view.item ?: return fail("Expected an item")
        assertEquals(fakeCredential.hostname, obs.hostname)
        assertEquals(fakeCredential.username, obs.username)
        assertEquals(fakeCredential.password, obs.password)
        assertEquals(fakeCredential.id, obs.id)
    }

    @Test
    fun `doesn't update UI when credential becomes null`() {
        clearInvocations(view)

        dataStore.getStub.onNext(Optional<ServerPassword>(null))

        verifyZeroInteractions(view)
    }

    @Test
    fun `opens a browser when tapping on the hostname`() {
        val clicks = view.hostnameClicks
        clicks.onNext(Unit)

        dispatcherObserver.assertLastValue(RouteAction.OpenWebsite(fakeCredential.hostname))
    }

    @Test
    fun `tapping on usernamecopy`() {
        view.usernameCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                NetworkAction.CheckConnectivity,
                ClipboardAction.CopyUsername(fakeCredential.username!!),
                DataStoreAction.Touch(fakeCredential.id)
            )
        )

        Assert.assertEquals(R.string.toast_username_copied, view.toastNotificationArgument)
    }

    @Test
    fun `tapping on passwordcopy`() {
        view.passwordCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                NetworkAction.CheckConnectivity,
                ClipboardAction.CopyPassword(fakeCredential.password),
                DataStoreAction.Touch(fakeCredential.id)
            )
        )

        Assert.assertEquals(R.string.toast_password_copied, view.toastNotificationArgument)
    }

    @Test
    fun `network error visibility is correctly being set`() {
        assertEquals(true, view.networkAvailable)
    }
}
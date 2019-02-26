/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.net.ConnectivityManager
import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.spy
import org.mockito.Mockito.verifyZeroInteractions
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ItemDetailPresenterTest {
    class FakeView : ItemDetailView {
        override var showUsernamePlaceholder: Boolean = false

        val learnMoreClickStub = PublishSubject.create<Unit>()
        override val learnMoreClicks: Observable<Unit>
            get() = learnMoreClickStub

//        private val retryButtonStub = PublishSubject.create<Unit>()
//        override val retryNetworkConnectionClicks: Observable<Unit>
//            get() = retryButtonStub

        var networkAvailable = PublishSubject.create<Boolean>()
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable.onNext(networkErrorVisibility)
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

    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()!!

    val view = spy(FakeView())
    private val itemDetailStore = ItemDetailStore(dispatcher)

    @Mock
    val networkStore = PowerMockito.mock(NetworkStore::class.java)!!

    private var isConnected: Observable<Boolean> = PublishSubject.create()
    var isConnectedObserver = TestObserver.create<Boolean>()

    @Mock
    private val connectivityManager = PowerMockito.mock(ConnectivityManager::class.java)

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

    private val fakeCredentialNoUsername: ServerPassword by lazy {
        ServerPassword(
            "id1",
            "https://www.mozilla.org",
            "",
            "woof",
            timesUsed = 0,
            timeCreated = 0L,
            timeLastUsed = 0L,
            timePasswordChanged = 0L
        )
    }

    lateinit var dataStore: FakeDataStore
    lateinit var subject: ItemDetailPresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        Mockito.`when`(networkStore.isConnected).thenReturn(isConnected)
        networkStore.connectivityManager = connectivityManager
        view.networkAvailable.subscribe(isConnectedObserver)
    }

    private fun setUpTestSubject(item: Optional<ServerPassword>) {
        dataStore = FakeDataStore()
        subject = ItemDetailPresenter(view, item.value?.id, dispatcher, networkStore, dataStore, itemDetailStore)
        subject.onViewReady()

        dataStore.getStub.onNext(item)
    }

    @Test
    fun `sends a detail view model to view`() {
        setUpTestSubject(fakeCredential.asOptional())

        // test the results that the view gets.
        val obs = view.item ?: return fail("Expected an item")
        assertEquals(fakeCredential.hostname, obs.hostname)
        assertEquals(fakeCredential.username, obs.username)
        assertEquals(fakeCredential.password, obs.password)
        assertEquals(fakeCredential.id, obs.id)
    }

    @Test
    fun `sends a detail view model to view with null username`() {
        setUpTestSubject(fakeCredentialNoUsername.asOptional())

        view.updateItem(
            ItemDetailViewModel(
                fakeCredentialNoUsername.id,
                fakeCredentialNoUsername.hostname,
                fakeCredentialNoUsername.hostname,
                fakeCredentialNoUsername.username,
                fakeCredentialNoUsername.password
            )
        )

        Assert.assertEquals(fakeCredentialNoUsername.id, dataStore.idArg)

        val obs = view.item ?: return fail("Expected an item")
        assertEquals(fakeCredentialNoUsername.hostname, obs.hostname)
        assertEquals(fakeCredentialNoUsername.username, obs.username)
        assertEquals(fakeCredentialNoUsername.password, obs.password)
        assertEquals(fakeCredentialNoUsername.id, obs.id)
    }

    @Test
    fun `correct formatting functions called with null username`() {
        setUpTestSubject(fakeCredentialNoUsername.asOptional())
        assertEquals(true, view.showUsernamePlaceholder)
    }

    @Test
    fun `correct formatting functions called with non-null username`() {
        setUpTestSubject(fakeCredential.asOptional())
        assertEquals(false, view.showUsernamePlaceholder)
    }

    @Test
    fun `doesn't update UI when credential becomes null`() {
        setUpTestSubject(Optional<ServerPassword>(null))

        clearInvocations(view)

        dataStore.getStub.onNext(Optional<ServerPassword>(null))

        verifyZeroInteractions(view)
    }

    @Test
    fun `opens a browser when tapping on the hostname`() {
        setUpTestSubject(fakeCredential.asOptional())

        val clicks = view.hostnameClicks
        clicks.onNext(Unit)

        dispatcherObserver.assertLastValue(RouteAction.OpenWebsite(fakeCredential.hostname))
    }

    @Test
    fun `tapping on usernamecopy`() {
        setUpTestSubject(fakeCredential.asOptional())
        view.usernameCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ClipboardAction.CopyUsername(fakeCredential.username!!),
                DataStoreAction.Touch(fakeCredential.id)
            )
        )

        Assert.assertEquals(R.string.toast_username_copied, view.toastNotificationArgument)
    }

    @Test
    fun `cannot copy username when null`() {
        setUpTestSubject(fakeCredentialNoUsername.asOptional())

        view.usernameCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            emptyList()
        )

        Assert.assertEquals(null, view.toastNotificationArgument)
    }

    @Test
    fun `tapping on passwordcopy`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.passwordCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ClipboardAction.CopyPassword(fakeCredential.password),
                DataStoreAction.Touch(fakeCredential.id)
            )
        )

        Assert.assertEquals(R.string.toast_password_copied, view.toastNotificationArgument)
    }

    @Test
    fun `tapping on togglepassword`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.togglePasswordClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(ItemDetailAction.TogglePassword(true))
        )
        Assert.assertTrue(view.isPasswordVisible)

        dispatcherObserver.values().clear()
        view.togglePasswordClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(ItemDetailAction.TogglePassword(false))
        )
        Assert.assertFalse(view.isPasswordVisible)
    }

    @Test
    fun `network error visibility is correctly being set`() {
        setUpTestSubject(fakeCredential.asOptional())

        val value = view.networkAvailable
        value.onNext(true)

        isConnectedObserver.assertValue(true)
    }

    @Test
    fun `learn more clicks`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.learnMoreClickStub.onNext(Unit)
        dispatcherObserver.assertLastValue(AppWebPageAction.FaqEdit)
    }
}
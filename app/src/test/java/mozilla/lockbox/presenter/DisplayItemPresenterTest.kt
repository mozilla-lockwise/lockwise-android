/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import android.net.ConnectivityManager
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.action.ClipboardAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.ToastNotificationAction
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
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class DisplayItemPresenterTest {

    class FakeView : DisplayItemView {
        override fun showKebabMenu() {}
        override fun hideKebabMenu() {}

        var editClicksStub: BehaviorRelay<Unit> = BehaviorRelay.createDefault(Unit)
        override val editClicks: Observable<Unit>
            get() = editClicksStub

        var deleteClicksStub: BehaviorRelay<Unit> = BehaviorRelay.createDefault(Unit)
        override val deleteClicks: Observable<Unit>
            get() = deleteClicksStub

        override fun showPopup() {}

        private val kebabMenuClickStub = PublishSubject.create<Unit>()
        override val kebabMenuClicks: Observable<Unit>
            get() = kebabMenuClickStub

//        private val retryButtonStub = PublishSubject.create<Unit>()
//        override val retryNetworkConnectionClicks: Observable<Unit>
//            get() = retryButtonStub

        var networkAvailable = PublishSubject.create<Boolean>()
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable.onNext(networkErrorVisibility)
        }

        var item: ItemDetailViewModel? = null

        var toastNotificationArgument: Int? = null

        override val usernameFieldClicks = PublishSubject.create<Unit>()
        override val usernameCopyClicks = PublishSubject.create<Unit>()
        override val passwordFieldClicks = PublishSubject.create<Unit>()
        override val passwordCopyClicks = PublishSubject.create<Unit>()
        override val togglePasswordClicks = PublishSubject.create<Unit>()
        override val hostnameClicks = PublishSubject.create<Unit>()
        override val launchButtonClicks = PublishSubject.create<Unit>()

        override var isPasswordVisible: Boolean = false

        var showPlaceholderUsernameStub: Boolean = false
        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
            showPlaceholderUsernameStub = !item.hasUsername
        }
    }

    @Mock
    val config: BuildConfig = PowerMockito.mock(BuildConfig::class.java)

    @Mock
    val dataStore: DataStore = PowerMockito.mock(DataStore::class.java)

    @Mock
    val networkStore = PowerMockito.mock(NetworkStore::class.java)!!

    @Mock
    private val connectivityManager = PowerMockito.mock(ConnectivityManager::class.java)

    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()!!
    val view: FakeView = spy(FakeView())

    private val getStub = PublishSubject.create<Optional<ServerPassword>>()
    private val itemDetailStore = ItemDetailStore(dataStore, dispatcher)

    private var isConnected: Observable<Boolean> = PublishSubject.create()
    private var isConnectedObserver: TestObserver<Boolean> = TestObserver.create<Boolean>()

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

    lateinit var subject: DisplayItemPresenter

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Before
    fun setUp() {
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments().thenReturn(dataStore)
        Mockito.`when`(networkStore.isConnected).thenReturn(isConnected)
        Mockito.`when`(dataStore.get(ArgumentMatchers.anyString())).thenReturn(getStub)
        networkStore.connectivityManager = connectivityManager
        view.networkAvailable.subscribe(isConnectedObserver)
    }

    private fun setUpTestSubject(item: Optional<ServerPassword>) {
        subject = DisplayItemPresenter(view, item.value?.id, dispatcher, networkStore, dataStore, itemDetailStore)
        subject.onViewReady()
        getStub.onNext(item)
        dispatcher.register.subscribe(dispatcherObserver)
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

        verify(dataStore).get(fakeCredentialNoUsername.id)

        val obs = view.item ?: return fail("Expected an item")
        assertEquals(fakeCredentialNoUsername.hostname, obs.hostname)
        assertEquals(fakeCredentialNoUsername.username, obs.username)
        assertEquals(fakeCredentialNoUsername.password, obs.password)
        assertEquals(fakeCredentialNoUsername.id, obs.id)
    }

    @Test
    fun `correct formatting functions called with null username`() {
        setUpTestSubject(fakeCredentialNoUsername.asOptional())
        assertEquals(true, view.showPlaceholderUsernameStub)
    }

    @Test
    fun `correct formatting functions called with non-null username`() {
        setUpTestSubject(fakeCredential.asOptional())
        assertEquals(false, view.showPlaceholderUsernameStub)
    }

    @Test
    fun `doesn't update UI when credential becomes null`() {
        setUpTestSubject(Optional(null))

        clearInvocations(view)

        getStub.onNext(Optional(null))

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
    fun `opens a browser when tapping on the hostname launch button`() {
        setUpTestSubject(fakeCredential.asOptional())

        val clicks = view.launchButtonClicks
        clicks.onNext(Unit)

        dispatcherObserver.assertLastValue(RouteAction.OpenWebsite(fakeCredential.hostname))
    }

    @Test
    fun `tapping on username copy button copies credentials`() {
        setUpTestSubject(fakeCredential.asOptional())
        view.usernameCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ClipboardAction.CopyUsername(fakeCredential.username!!),
                DataStoreAction.Touch(fakeCredential.id),
                ToastNotificationAction.ShowCopyUsernameToast
            )
        )
    }

    @Test
    fun `tapping on username field copies credentials`() {
        setUpTestSubject(fakeCredential.asOptional())
        view.usernameFieldClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ClipboardAction.CopyUsername(fakeCredential.username!!),
                DataStoreAction.Touch(fakeCredential.id),
                ToastNotificationAction.ShowCopyUsernameToast
            )
        )
    }

    @Test
    fun `cannot copy username when null`() {
        setUpTestSubject(fakeCredentialNoUsername.asOptional())

        view.usernameCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            emptyList()
        )
    }

    @Test
    fun `tapping on password copy button copies the password`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.passwordCopyClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ClipboardAction.CopyPassword(fakeCredential.password),
                DataStoreAction.Touch(fakeCredential.id),
                ToastNotificationAction.ShowCopyPasswordToast
            )
        )
    }

    @Test
    fun `tapping on password field copies the password`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.passwordFieldClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ClipboardAction.CopyPassword(fakeCredential.password),
                DataStoreAction.Touch(fakeCredential.id),
                ToastNotificationAction.ShowCopyPasswordToast
            )
        )
    }

    @Test
    fun `tapping on togglepassword`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.togglePasswordClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(ItemDetailAction.SetPasswordVisibility(true))
        )
        Assert.assertTrue(view.isPasswordVisible)

        dispatcherObserver.values().clear()
        view.togglePasswordClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(ItemDetailAction.SetPasswordVisibility(false))
        )
        Assert.assertFalse(view.isPasswordVisible)
    }

    @Test
    fun `password visibility when app is paused in background`() {
        setUpTestSubject(fakeCredential.asOptional())
        // set password as visible
        view.togglePasswordClicks.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(ItemDetailAction.SetPasswordVisibility(true))
        )

        Assert.assertTrue(view.isPasswordVisible)

        // pause background the app
        subject.onPause()

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
    fun `select delete from kebab menu`() {
        setUpTestSubject(fakeCredential.asOptional())
        view.deleteClicksStub.accept(Unit)
        dispatcherObserver.assertValue(DialogAction.DeleteConfirmationDialog(fakeCredential))
    }

    @Test
    fun `select edit from kebab menu`() {
        setUpTestSubject(fakeCredential.asOptional())
        view.editClicksStub.accept(Unit)
        dispatcherObserver.assertValue(RouteAction.EditItem(fakeCredential.id))
    }
}

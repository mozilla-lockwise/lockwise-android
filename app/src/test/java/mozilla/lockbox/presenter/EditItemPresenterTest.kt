/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.ItemDetailStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class EditItemPresenterTest {

    class FakeView : EditItemDetailView {

        private val togglePasswordVisibilityStub = PublishSubject.create<Unit>()
        override val togglePasswordVisibility: Observable<Unit>
            get() = togglePasswordVisibilityStub

        private val passwordVisibleStub = false
        override var isPasswordVisible: Boolean = passwordVisibleStub

        private val togglePwdClicksStub = PublishSubject.create<Unit>()
        override val togglePasswordClicks: Observable<Unit>
            get() = togglePwdClicksStub

        private val hostnameClicksStub = PublishSubject.create<String>()
        override val hostnameChanged: Observable<String>
            get() = hostnameClicksStub

        val usernameClicksStub = PublishSubject.create<String>()
        override val usernameChanged: Observable<String>
            get() = usernameClicksStub

        val pwdClicksStub = PublishSubject.create<String>()
        override val passwordChanged: Observable<String>
            get() = pwdClicksStub

        override fun closeKeyboard() {
            log.info("close keyboard")
        }

        var item: ItemDetailViewModel? = null
        @StringRes
        var usernameError: Int? = null
        @StringRes
        var passwordError: Int? = null
        var _saveEnabled = true

        val closeEntryClicksStub = PublishSubject.create<Unit>()
        override val closeEntryClicks: Observable<Unit>
            get() = closeEntryClicksStub

        val saveEntryClicksStub = PublishSubject.create<Unit>()
        override val saveEntryClicks: Observable<Unit>
            get() = saveEntryClicksStub

        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
        }

        override fun displayUsernameError(@StringRes errorMessage: Int?) {
            usernameError = errorMessage
        }

        override fun displayPasswordError(@StringRes errorMessage: Int?) {
            passwordError = errorMessage
        }

        override fun setSaveEnabled(enabled: Boolean) {
            _saveEnabled = enabled
        }
    }

    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!
    private val getStub = PublishSubject.create<Optional<ServerPassword>>()
    private val listStub = PublishSubject.create<List<ServerPassword>>()

    val itemDetailStore = PowerMockito.mock(ItemDetailStore::class.java)!!
    private val isPasswordVisibleStub = PublishSubject.create<Boolean>()
    private val isEditingStub = PublishSubject.create<Boolean>()

    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()!!

    val view: FakeView = spy(FakeView())

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

    private val duplicateFakeCredential: ServerPassword by lazy {
        ServerPassword(
            "id2",
            "https://www.mozilla.org",
            "dogs@dogs.com",
            "woofwoof",
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

    lateinit var subject: EditItemPresenter

    @Before
    fun setUp() {
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments().thenReturn(dataStore)
        dispatcher.register.subscribe(dispatcherObserver)
        Mockito.`when`(dataStore.get(ArgumentMatchers.anyString())).thenReturn(getStub)
        Mockito.`when`(dataStore.list).thenReturn(listStub)

        Mockito.`when`(itemDetailStore.isPasswordVisible).thenReturn(isPasswordVisibleStub)
        Mockito.`when`(itemDetailStore.isEditing).thenReturn(isEditingStub)

        isEditingStub.onNext(true)
    }

    private fun setUpTestSubject(item: ServerPassword?) {
        subject = EditItemPresenter(view, item?.id, dispatcher, dataStore, itemDetailStore)
        subject.onViewReady()

        getStub.onNext(item.asOptional())
        listStub.onNext(
            listOf(
                fakeCredential,
                fakeCredentialNoUsername
            )
        )
    }

    @Test
    fun `sends a detail view model to view`() {
        setUpTestSubject(fakeCredential)

        // test the results that the view gets.
        val obs = view.item ?: return fail("Expected an item")
        assertEquals(fakeCredential.hostname, obs.hostname)
        assertEquals(fakeCredential.username, obs.username)
        assertEquals(fakeCredential.password, obs.password)
        assertEquals(fakeCredential.id, obs.id)
    }

    @Test
    fun `sends a detail view model to view with null username`() {
        setUpTestSubject(fakeCredentialNoUsername)

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
    fun `sends a list of duplicates to the view model`() {
        setUpTestSubject(fakeCredentialNoUsername)
        view.usernameClicksStub.onNext("")
        view.pwdClicksStub.onNext(fakeCredentialNoUsername.password)

        // now do the test.
        view.usernameClicksStub.onNext(fakeCredential.username ?: "")

        assertNotNull(view.usernameError)
    }

    @Test
    fun `tapping on close button with no change`() {
        setUpTestSubject(fakeCredential)

        view.closeEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndEditing(fakeCredential.id)
            )
        )
    }

    @Test
    fun `tapping on close button with change`() {
        setUpTestSubject(fakeCredential)

        view.usernameClicksStub.onNext("all-change")
        view.closeEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                DialogAction.DiscardChangesDialog(fakeCredential.id)
            )
        )
    }

    @Test
    fun `tapping on save button with no changes`() {
        setUpTestSubject(fakeCredential)

        view.saveEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.EndEditing(fakeCredential.id)
            )
        )
    }

    @Test
    fun `tapping on save button`() {
        setUpTestSubject(fakeCredential)

        view.usernameClicksStub.onNext("all-change")
        view.saveEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                ItemDetailAction.SaveChanges(fakeCredential, fakeCredential.copy(username = "all-change"))
            )
        )
    }

    @Test
    fun `stopping editing sends you back to the item detail`() {
        setUpTestSubject(fakeCredential)
        isEditingStub.onNext(false)

        dispatcherObserver.assertValueSequence(
            listOf(
                RouteAction.ItemList,
                RouteAction.ItemDetail(fakeCredential.id)
            )
        )
    }
}

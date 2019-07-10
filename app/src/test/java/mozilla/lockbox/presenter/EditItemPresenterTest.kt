/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
import mozilla.lockbox.action.DialogAction
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
class EditItemPresenterTest {

    class FakeView : EditItemDetailView {

        var item: ItemDetailViewModel? = null

        val deleteClicksStub = PublishSubject.create<Unit>()
        override val deleteClicks: Observable<Unit>
            get() = deleteClicksStub

        val closeEntryClicksStub = PublishSubject.create<Unit>()
        override val closeEntryClicks: Observable<Unit>
            get() = closeEntryClicksStub

        val saveEntryClicksStub = PublishSubject.create<Unit>()
        override val saveEntryClicks: Observable<Unit>
            get() = saveEntryClicksStub

        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
        }
    }

    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!
    private val getStub = PublishSubject.create<Optional<ServerPassword>>()

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
    }

    private fun setUpTestSubject(item: Optional<ServerPassword>) {
        subject = EditItemPresenter(view, item.value?.id, dispatcher, dataStore)
        subject.onViewReady()

        getStub.onNext(item)
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
    fun `tapping on close button`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.closeEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                DialogAction.DiscardChangesDialog(fakeCredential.id)
            )
        )
    }

    @Test
    fun `tapping on save button`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.saveEntryClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                DataStoreAction.UpdateItemDetail(fakeCredential),
                RouteAction.ItemDetail(fakeCredential.id)
            )
        )
    }

    @Test
    fun `tapping on delete button`() {
        setUpTestSubject(fakeCredential.asOptional())

        view.deleteClicksStub.onNext(Unit)

        dispatcherObserver.assertValueSequence(
            listOf(
                DataStoreAction.Delete(fakeCredential),
                RouteAction.ItemList
            )
        )
    }
}
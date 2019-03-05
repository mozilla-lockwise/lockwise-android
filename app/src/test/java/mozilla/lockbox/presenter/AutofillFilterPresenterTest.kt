/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.TestConsumer
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.extensions.toViewModel
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AutofillFilterPresenterTest {
    class FakeView : AutofillFilterView {
        val onDismissStub = PublishSubject.create<Unit>()
        val filterTextEnteredStub = PublishSubject.create<CharSequence>()
        val filterTextSetStub = TestObserver<CharSequence>()
        val cancelButtonStub = PublishSubject.create<Unit>()
        val cancelButtonVisibilityStub = TestObserver<Boolean>()

        val itemSelectionStub = PublishSubject.create<ItemViewModel>()
        var updateItemsArgument: List<ItemViewModel>? = null
        var updateItemsStatus: Boolean? = null

        override val onDismiss: Observable<Unit> = onDismissStub
        override val filterTextEntered: Observable<CharSequence> = filterTextEnteredStub
        override val filterText: Consumer<in CharSequence> = TestConsumer(filterTextSetStub)
        override val cancelButtonClicks: Observable<Unit> = cancelButtonStub

        override val cancelButtonVisibility: Consumer<in Boolean> = TestConsumer(cancelButtonVisibilityStub)

        override val itemSelection: Observable<ItemViewModel> = itemSelectionStub
        override fun updateItems(items: List<ItemViewModel>, textEntered: Boolean) {
            updateItemsArgument = items
            updateItemsStatus = textEntered
        }
    }

    class FakeDataStore : DataStore() {
        val listStub = PublishSubject.create<List<ServerPassword>>()
        override val list: Observable<List<ServerPassword>>
            get() = listStub

        var getArgument: String? = null
        val getStub = PublishSubject.create<Optional<ServerPassword>>()
        override fun get(id: String): Observable<Optional<ServerPassword>> {
            getArgument = id
            return getStub
        }
    }

    private val serverPassword1 = ServerPassword(
        "oiupkjkui",
        "www.mozilla.org",
        username = "cats@cats.com",
        password = "woof"
    )

    private val serverPassword2 = ServerPassword(
        "ljklkjldfs",
        "www.neopets.com",
        username = "dogs@dogs.com",
        password = "meow"
    )

    private val items = listOf(serverPassword1, serverPassword2)

    private val view = FakeView()
    private val dispatcher = Dispatcher()
    private val dataStore = FakeDataStore()
    private val dispatcherObserver: TestObserver<Action> = TestObserver.create()

    val subject = AutofillFilterPresenter(view, dispatcher, dataStore)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        subject.onViewReady()
    }

    @Test
    fun `dismiss stub cancels`() {
        view.onDismissStub.onNext(Unit)

        dispatcherObserver.assertValue { it is AutofillAction.Cancel }
    }

    @Test
    fun `item list without text`() {
        dataStore.listStub.onNext(items)
        view.filterTextEnteredStub.onNext("")

        assertFalse(view.updateItemsStatus!!)
        assertEquals(emptyList<ItemViewModel>(), view.updateItemsArgument!!)
        view.cancelButtonVisibilityStub.assertValue(false)
    }

    @Test
    fun `item list with text matching username`() {
        dataStore.listStub.onNext(items)
        view.filterTextEnteredStub.onNext("cat")

        assertTrue(view.updateItemsStatus!!)
        assertEquals(listOf(serverPassword1.toViewModel()), view.updateItemsArgument)
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `item list with text matching domain`() {
        dataStore.listStub.onNext(items)
        view.filterTextEnteredStub.onNext("neo")

        assertTrue(view.updateItemsStatus!!)
        assertEquals(listOf(serverPassword2.toViewModel()), view.updateItemsArgument)
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `item list with no matches`() {
        dataStore.listStub.onNext(items)
        view.filterTextEnteredStub.onNext("vvvvvvvvvvv")

        assertTrue(view.updateItemsStatus!!)
        assertEquals(emptyList<ItemViewModel>(), view.updateItemsArgument)
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `item selection with datastore entry`() {
        val guid = "fdssdffdsfdssdf"
        view.itemSelectionStub.onNext(ItemViewModel("meh", "blah", guid))

        assertEquals(guid, dataStore.getArgument)

        dataStore.getStub.onNext(serverPassword1.asOptional())

        val autofillAction = dispatcherObserver.values().last() as AutofillAction.Complete

        assertEquals(serverPassword1, autofillAction.login)
    }

    @Test
    fun `item selection without datastore entry`() {
        // ^ test case unlikely
        val guid = "fdssdffdsfdssdf"
        view.itemSelectionStub.onNext(ItemViewModel("meh", "blah", guid))

        assertEquals(guid, dataStore.getArgument)

        dataStore.getStub.onNext(Optional(null))

        dispatcherObserver.assertValue(AutofillAction.Cancel)
    }

    @Test
    fun `cancel button clicks`() {
        view.cancelButtonStub.onNext(Unit)

        view.filterTextSetStub.assertValue("")
    }
}
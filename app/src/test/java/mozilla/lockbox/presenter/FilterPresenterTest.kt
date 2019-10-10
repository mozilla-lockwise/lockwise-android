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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.TestConsumer
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.toViewModel
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FilterPresenterTest {
    class FakeFilterView : FilterView {
        override val onDismiss: Observable<Unit>?
            get() = null
        override val displayNoEntries: ((Boolean) -> Unit)?
            get() = null
        val filterTextEnteredStub = PublishSubject.create<CharSequence>()
        val filterTextSetStub = TestObserver<CharSequence>()
        val cancelButtonStub = PublishSubject.create<Unit>()
        val cancelButtonVisibilityStub = TestObserver<Boolean>()
        val itemSelectionStub = PublishSubject.create<ItemViewModel>()

        var updateItemsArgument: List<ItemViewModel>? = null

        override val filterTextEntered: Observable<CharSequence> = filterTextEnteredStub
        override val filterText: Consumer<in CharSequence> = TestConsumer(filterTextSetStub)
        override val cancelButtonClicks: Observable<Unit> = cancelButtonStub
        override val cancelButtonVisibility: Consumer<in Boolean> =
            TestConsumer(cancelButtonVisibilityStub)
        override val itemSelection: Observable<ItemViewModel> = itemSelectionStub

        override fun updateItems(items: List<ItemViewModel>) {
            updateItemsArgument = items
        }
    }

    class FakeFilterPresenter(
        override val view: FilterView,
        override val dispatcher: Dispatcher,
        override val dataStore: DataStore
    ) : FilterPresenter(view, dispatcher, dataStore) {
        val itemSelectionActionSubject = PublishSubject.create<Action>()

        override fun Observable<ItemViewModel>.itemSelectionActionMap(): Observable<Action> {
            return itemSelectionActionSubject
        }

        override fun Observable<Pair<CharSequence, List<ItemViewModel>>>.itemListMap(): Observable<List<ItemViewModel>> {
            return this.map { it.second }
        }
    }

    private val listStub = PublishSubject.create<List<ServerPassword>>()
    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!

    val view = FakeFilterView()
    val dispatcher = Dispatcher()
    lateinit var subject: FakeFilterPresenter

    val dispatcherObserver: TestObserver<Action> = TestObserver.create<Action>()

    val serverPassword1 = ServerPassword(
        "oiupkjkui",
        "www.mozilla.org",
        username = "cats@cats.com",
        password = "woof"
    )
    val serverPassword2 = ServerPassword(
        "ljklkjldfs",
        "www.neopets.com",
        username = "dogs@dogs.com",
        password = "meow"
    )

    private val items = listOf(serverPassword1, serverPassword2)

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        whenCalled(dataStore.list).thenReturn(listStub)
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments().thenReturn(dataStore)

        subject = FakeFilterPresenter(view, dispatcher, dataStore)
        subject.onViewReady()
        listStub.onNext(items)
    }

    @Test
    fun `filter text is empty`() {
        view.filterTextEnteredStub.onNext("")

        Assert.assertEquals(view.updateItemsArgument, items.map { it.toViewModel() })
        view.cancelButtonVisibilityStub.assertValue(false)
    }

    @Test
    fun `filter text with matching username`() {
        view.filterTextEnteredStub.onNext("cat")

        Assert.assertEquals(view.updateItemsArgument, listOf(serverPassword1.toViewModel()))
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `filter text with matching password`() {
        view.filterTextEnteredStub.onNext("woo")

        Assert.assertEquals(view.updateItemsArgument, emptyList<ItemViewModel>())
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `filter text with matching domain`() {
        view.filterTextEnteredStub.onNext("neo")

        Assert.assertEquals(view.updateItemsArgument, listOf(serverPassword2.toViewModel()))
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `filter text populated with matching guid`() {
        view.filterTextEnteredStub.onNext("ljk")

        Assert.assertEquals(view.updateItemsArgument, emptyList<ItemViewModel>())
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `filter text populated with matching unedited hostname`() {
        view.filterTextEnteredStub.onNext("www")

        Assert.assertEquals(view.updateItemsArgument, emptyList<ItemViewModel>())
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun `cancel button clicks`() {
        view.cancelButtonStub.onNext(Unit)
        view.filterTextSetStub.assertValue("")
    }

    @Test
    fun `select item from filter`() {
        val guid = "fdssdfsdf"
        val model = ItemViewModel("mozilla.org", "cats@cats.com", guid)
        view.itemSelectionStub.onNext(model)
        val action = RouteAction.LockScreen

        subject.itemSelectionActionSubject.onNext(action)

        dispatcherObserver.assertValue(action)
    }
}
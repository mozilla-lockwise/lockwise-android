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
import org.mozilla.sync15.logins.ServerPassword
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class FilterPresenterTest {
    class FakeView : FilterView {
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

    class FakeDataStore : DataStore() {
        val listStub = PublishSubject.create<List<ServerPassword>>()
        override val list = listStub
    }

    val view = FakeView()
    val dataStore = FakeDataStore()
    val subject = FilterPresenter(view, dataStore = dataStore)

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
    val items = listOf(serverPassword1, serverPassword2)

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        subject.onViewReady()
        dataStore.listStub.onNext(items)
    }

    @Test
    fun filterText_empty() {
        view.filterTextEnteredStub.onNext("")

        Assert.assertEquals(view.updateItemsArgument, items.map { it.toViewModel() })
        view.cancelButtonVisibilityStub.assertValue(false)
    }

    @Test
    fun filterText_populated_matchesUsername() {
        view.filterTextEnteredStub.onNext("cat")

        Assert.assertEquals(view.updateItemsArgument, listOf(serverPassword1.toViewModel()))
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun filterText_populated_matchesPassword() {
        view.filterTextEnteredStub.onNext("woo")

        Assert.assertEquals(view.updateItemsArgument, emptyList<ItemViewModel>())
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun filterText_populated_matchesDomain() {
        view.filterTextEnteredStub.onNext("neo")

        Assert.assertEquals(view.updateItemsArgument, listOf(serverPassword2.toViewModel()))
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun filterText_populated_matchesGuid() {
        view.filterTextEnteredStub.onNext("ljk")

        Assert.assertEquals(view.updateItemsArgument, emptyList<ItemViewModel>())
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun filterText_populated_matchesUneditedHostname() {
        view.filterTextEnteredStub.onNext("www")

        Assert.assertEquals(view.updateItemsArgument, emptyList<ItemViewModel>())
        view.cancelButtonVisibilityStub.assertValue(true)
    }

    @Test
    fun cancelButtonClicks() {
        view.cancelButtonStub.onNext(Unit)

        view.filterTextSetStub.assertValue("")
    }

    @Test
    fun itemSelection() {
        val guid = "fdssdfsdf"
        val model = ItemViewModel("mozilla.org", "cats@cats.com", guid, Date().time)
        view.itemSelectionStub.onNext(model)

        dispatcherObserver.assertValue(RouteAction.ItemDetail(guid))
    }
}
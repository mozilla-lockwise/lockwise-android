/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import mozilla.lockbox.R
import mozilla.lockbox.action.ItemListSort
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.rx.ListItem
import mozilla.lockbox.store.DataStore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.sync15.logins.ServerPassword
import org.robolectric.RobolectricTestRunner
import java.util.Date

private val username = "dogs@dogs.com"
private val password1 = ServerPassword(
    "fdsfda",
    "https://www.mozilla.org",
    username,
    "woof",
    timesUsed = 0,
    timeCreated = 0L,
    timeLastUsed = 1L,
    timePasswordChanged = 0L)
private val password2 = ServerPassword("ghfdhg",
    "https://www.cats.org",
    username,
    "meow",
    timesUsed = 0,
    timeCreated = 0L,
    timeLastUsed = 2L,
    timePasswordChanged = 0L)
private val password3 = ServerPassword("ioupiouiuy",
    "www.dogs.org",
    password = "baaaaa",
    username = null,
    timesUsed = 0,
    timeCreated = 0L,
    timeLastUsed = 3L,
    timePasswordChanged = 0L)

@RunWith(RobolectricTestRunner::class)
class ItemListPresenterTest {
    class FakeView : ItemListView {
        val itemSelectedStub = PublishSubject.create<ItemViewModel>()
        val filterClickStub = PublishSubject.create<Unit>()
        val menuItemSelectionStub = PublishSubject.create<Int>()
        val sortItemSelectionStub = PublishSubject.create<ItemListSort>()

        var updateItemsArgument: List<ItemViewModel>? = null
        var itemListSort: ItemListSort? = null

        override val itemSelection: Observable<ItemViewModel>
            get() = itemSelectedStub

        override val sortItemSelection: Observable<ItemListSort>
            get() = sortItemSelectionStub

        override val filterClicks: Observable<Unit>
            get() = filterClickStub

        override val menuItemSelections: Observable<Int>
            get() = menuItemSelectionStub

        override fun updateItems(itemList: List<ItemViewModel>) {
            updateItemsArgument = itemList
        }

        override fun updateItemListSort(sort: ItemListSort) {
            itemListSort = sort
        }
    }

    class FakeDataStore : DataStore() {
        val listStub = PublishSubject.create<List<ServerPassword>>()

        override val list: Observable<List<ServerPassword>>
            get() = listStub
    }

    val view = FakeView()
    val dataStore = FakeDataStore()
    val subject = ItemListPresenter(view, dataStore = dataStore)

    val dispatcherObserver = TestObserver.create<Action>()

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

        subject.onViewReady()
    }

    @Test
    fun receivingItemSelected() {
        val id = "the_guid"
        view.itemSelectedStub.onNext(ItemViewModel("title", "subtitle", id, Date().time))

        dispatcherObserver.assertLastValue(RouteAction.ItemDetail(id))
    }

    @Test
    fun receivingPasswordList_somePasswords() {
        val list = listOf(password1, password2, password3)
        dataStore.listStub.onNext(list)
        val expectedList = listOf(password2, password3, password1).map { it.toViewModel() }

        assertEquals(expectedList, view.updateItemsArgument)
        assertEquals(ItemListSort.ALPHABETICALLY, view.itemListSort)
        dataStore.listStub.onNext(list)

        val expectedList = listOf<ItemViewModel>(
                ItemViewModel("mozilla.org",
                        username,
                        password1.id,
                        Date().time),
                ItemViewModel("cats.org",
                        username,
                        password2.id,
                        Date().time),
                ItemViewModel("dogs.org",
                        "",
                        password3.id,
                        Date().time)
        )

        Assert.assertEquals(expectedList, view.updateItemsArgument)
    }

    @Test
    fun receivingPasswordList_empty() {
        dataStore.listStub.onNext(emptyList())

        Assert.assertNull(view.updateItemsArgument)
    }

    @Test
    fun `menuItem clicks cause RouteActions`() {
        view.menuItemSelectionStub.onNext(R.id.fragment_setting)
        dispatcherObserver.assertLastValue(RouteAction.SettingList)

        view.menuItemSelectionStub.onNext(R.id.fragment_locked)
        dispatcherObserver.assertLastValue(RouteAction.LockScreen)
    }
}

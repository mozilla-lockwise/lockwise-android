/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.view.MenuItem
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import junit.framework.Assert
import mozilla.lockbox.extensions.titleFromHostname
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.DataStore
import org.apache.maven.settings.Server
import org.junit.Before
import org.junit.Test
import org.mozilla.sync15.logins.ServerPassword

class ItemListPresenterTest {
    class FakeView : ItemListView {
        val drawerItemStub = PublishSubject.create<MenuItem>()
        var closeDrawersCalled = false
        var updateItemsArgument : List<ItemViewModel>? = null

        override val drawerItemSelections: Observable<MenuItem>
            get() = drawerItemStub

        override fun closeDrawers() {
            this.closeDrawersCalled = true
        }

        override fun updateItems(itemList: List<ItemViewModel>) {
            updateItemsArgument = itemList
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
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        subject.onViewReady()
    }

    @Test
    fun receivingDrawerItemSelections_settings() {
        // tbd: constructing menu items
    }

    @Test
    fun receivingDrawerItemSelections_lock_now() {
        // tbd: constructing menu items
    }

    @Test
    fun receivingPasswordList_somePasswords() {
        val username = "dogs@dogs.com"
        val password1 = ServerPassword(
                "fdsfda",
                "https://www.mozilla.org",
                username,
                "woof",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val password2 = ServerPassword("ghfdhg",
                "https://www.cats.org",
                username,
                "meow",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val password3 = ServerPassword("ioupiouiuy",
                "www.dogs.org",
                password = "baaaaa",
                username = null,
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val list = listOf(password1, password2, password3)

        dataStore.listStub.onNext(list)

        val expectedList = listOf<ItemViewModel>(
                ItemViewModel(password1.hostname.titleFromHostname(),
                        username,
                        password1.id),
                ItemViewModel(password2.hostname.titleFromHostname(),
                        username,
                        password2.id),
                ItemViewModel(password3.hostname.titleFromHostname(),
                        "",
                        password3.id)
        )

        Assert.assertEquals(expectedList, view.updateItemsArgument)
    }

    @Test
    fun receivingPasswordList_empty() {
        dataStore.listStub.onNext(emptyList())

        Assert.assertNull(view.updateItemsArgument)
    }

}
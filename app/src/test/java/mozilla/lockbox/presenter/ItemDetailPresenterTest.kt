/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.ClipboardManager
import android.widget.EditText
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mozilla.sync15.logins.ServerPassword
import org.mockito.Mockito

class ItemDetailPresenterTest {
    class FakeView : ItemDetailView {

        override var itemId: String? = null
        var item: ItemDetailViewModel? = null
        val tapStub: PublishSubject<Unit> = PublishSubject.create<Unit>()

        override val btnUsernameCopyClicks: Observable<Unit>
            get() = tapStub

        override val btnPasswordCopyClicks: Observable<Unit>
            get() = tapStub

        override val btnTogglePasswordClicks: Observable<Unit>
            get() = tapStub

        override val editUsername: EditText
            get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.
        override val editPassword: EditText
            get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
        }

        override fun copyNotification(strId: Int) {
        }

        override fun updatePasswordField(visible: Boolean) {
        }
    }

    class FakeDataStore : DataStore() {
        val listStub = PublishSubject.create<List<ServerPassword>>()

        override val list: Observable<List<ServerPassword>>
            get() = listStub
    }

    val view = FakeView()
    val dataStore = FakeDataStore()
    val clipboardManager = Mockito.mock(ClipboardManager::class.java)

    val subject = ItemDetailPresenter(view, clipboardManager, dataStore = dataStore)

    val dispatcherObserver = TestObserver.create<Action>()

    @Before
    fun setUp() {
        Dispatcher.shared.register.subscribe(dispatcherObserver)

        subject.onViewReady()
    }

    @Test
    fun testServerPasswordDeliveredToView() {
        val username = "dogs@dogs.com"
        val id1 = "id1"
        val password1 = ServerPassword(
                "id0",
                "https://www.mozilla.org",
                username,
                "woof",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val password2 = ServerPassword(id1,
                "https://www.cats.org",
                username,
                "meow",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)
        val password3 = ServerPassword("id2",
                "www.dogs.org",
                password = "baaaaa",
                username = null,
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L)

        val list = listOf(password1, password2, password3)

        for (exp in list) {
            // put the presenter/view on screen.
            view.itemId = exp.id
            subject.onResume()

            // drive the fake datastore.
            dataStore.listStub.onNext(list)

            // test the results that the view gets.
            val obs = view.item ?: return fail("Expected an item")
            assertEquals(exp.hostname, obs.hostname)
            assertEquals(exp.username, obs.username)
            assertEquals(exp.password, obs.password)
            assertEquals(exp.id, obs.id)
        }
    }
}
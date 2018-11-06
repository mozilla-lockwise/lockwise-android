/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.support.annotation.StringRes
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.ItemDetailViewModel
import mozilla.lockbox.store.DataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.sync15.logins.ServerPassword
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemDetailPresenterTest {
    class FakeView : ItemDetailView {

        var item: ItemDetailViewModel? = null

        override val usernameCopyClicks = PublishSubject.create<Unit>()
        override val passwordCopyClicks = PublishSubject.create<Unit>()
        override val togglePasswordClicks = PublishSubject.create<Unit>()
        override val hostnameClicks = PublishSubject.create<Unit>()

        override var isPasswordVisible: Boolean = false

        override fun updateItem(item: ItemDetailViewModel) {
            this.item = item
        }

        override fun showToastNotification(@StringRes strId: Int) {
            // notification Test
        }
    }

    class FakeDataStore : DataStore() {
        override val list = PublishSubject.create<List<ServerPassword>>()
    }

    val view = FakeView()
    val dataStore = FakeDataStore()
    val dispatcher = Dispatcher()
    val dispatcherObserver = TestObserver.create<Action>()

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
    }

    private val fakeCredentials: List<ServerPassword> by lazy {
        listOf(
            ServerPassword(
                "id0",
                "https://www.mozilla.org",
                "dogs@dogs.com",
                "woof",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L
            ), ServerPassword(
                "id1",
                "https://www.cats.org",
                "dogs@dogs.com",
                "meow",
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L
            ), ServerPassword(
                "id2",
                "www.dogs.org",
                password = "baaaaa",
                username = null,
                timesUsed = 0,
                timeCreated = 0L,
                timeLastUsed = 0L,
                timePasswordChanged = 0L
            )
        )
    }

    @Test
    fun `sends a detail view model to view on onViewReady`() {
        for (exp in fakeCredentials) {
            // put the presenter/accountSettingView on screen.
            val subject = ItemDetailPresenter(view, exp.id, dispatcher, dataStore)
            subject.onViewReady()

            // drive the fake datastore.
            dataStore.list.onNext(fakeCredentials)

            // test the results that the accountSettingView gets.
            val obs = view.item ?: return fail("Expected an item")
            assertEquals(exp.hostname, obs.hostname)
            assertEquals(exp.username, obs.username)
            assertEquals(exp.password, obs.password)
            assertEquals(exp.id, obs.id)
        }
    }

    @Test
    fun `opens a browser when tapping on the hostname`() {
        for (exp in fakeCredentials) {
            // put the presenter/accountSettingView on screen.
            val subject = ItemDetailPresenter(view, exp.id, dispatcher, dataStore)
            subject.onViewReady()

            // drive the fake datastore.
            dataStore.list.onNext(fakeCredentials)

            val clicks = view.hostnameClicks
            clicks.onNext(Unit)

            val last = dispatcherObserver.valueCount() - 1
            dispatcherObserver.assertValueAt(last, RouteAction.OpenWebsite(exp.hostname))
        }
    }
}
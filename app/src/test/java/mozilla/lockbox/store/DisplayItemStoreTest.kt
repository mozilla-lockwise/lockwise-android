/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.powermock.api.mockito.PowerMockito

@ExperimentalCoroutinesApi
class DisplayItemStoreTest : DisposingTest() {
    val dispatcher = Dispatcher()
    val dispatcherObserver = createTestObserver<Action>()

    val getStub = PublishSubject.create<Optional<ServerPassword>>()
    val listStub = ReplaySubject.createWithSize<List<ServerPassword>>(1)
    val dataStore by lazy {
        val store = PowerMockito.mock(DataStore::class.java)
        `when`(store.list).thenReturn(listStub)
        `when`(store.get(anyString())).thenReturn(getStub)
        store
    }

    val subject = ItemDetailStore(dataStore, dispatcher)

    @Test
    fun `test initial state`() {
        subject.isPasswordVisible.take(1).subscribe { visible ->
            Assert.assertFalse(visible)
        }.addTo(disposer)
    }

    @Test
    fun `routing to ItemList resets state`() {
        val observer = createTestObserver<Boolean>()

        subject.apply {
            _passwordVisible.onNext(true)
            isPasswordVisible.subscribe(observer)
        }

        dispatcher.dispatch(RouteAction.ItemList)
        observer.assertLastValue(false)
    }
    @Test
    fun `routing to ItemDetail resets state`() {
        val observer = createTestObserver<Boolean>()

        subject.apply {
            _passwordVisible.onNext(true)
            isPasswordVisible.subscribe(observer)
        }

        dispatcher.dispatch(RouteAction.DisplayItem("id00000"))
        observer.assertLastValue(false)
    }

    @Test
    fun `toggles passwordVisible from action`() {
        val observer = createTestObserver<Boolean>()
        subject.isPasswordVisible.subscribe(observer)

        dispatcher.dispatch(ItemDetailAction.SetPasswordVisibility(true))
        observer.assertLastValue(true)

        dispatcher.dispatch(ItemDetailAction.SetPasswordVisibility(false))
        observer.assertLastValue(false)
    }

    @Test
    fun `isEditing is toggled via actions`() {
        val observer = createTestObserver<Boolean>()
        subject.isEditing.subscribe(observer)

        val itemId = "id"
        dispatcher.dispatch(ItemDetailAction.BeginEditItemSession(itemId))
        observer.assertLastValue(true)

        dispatcher.dispatch(ItemDetailAction.EndEditItemSession)
        observer.assertLastValue(false)
    }

    @Test
    fun `save changes is achieved by actions`() {
        val observer = createTestObserver<ServerPassword>()
        dispatcher.register
            .filterByType(DataStoreAction.UpdateItemDetail::class.java)
            .map { it.next }
            .subscribe(observer)

        val itemId = "id"
        val original = ServerPassword(id = itemId,
            hostname = "hostname.com",
            password = "password"
        )

        dispatcher.dispatch(ItemDetailAction.BeginEditItemSession(itemId))
        getStub.onNext(original.asOptional())
        dispatcher.dispatch(ItemDetailAction.EditField(password = "password1"))
        dispatcher.dispatch(ItemDetailAction.EditItemSaveChanges)

        val mutated = original.copy(password = "password1")
        observer.assertLastValue(mutated)
    }

    fun logins(hostname: String, usernames: List<String>) =
        usernames.map {
            ServerPassword(
                id = "",
                username = it,
                hostname = hostname,
                password = "",
                formSubmitURL = hostname)
        }

    @Test
    fun `on creation, unavailableUsernames are calculated dynamically`() {
        val list =
            logins("hostname1", listOf("foo", "bar", "baz")) +
            logins("hostname2", listOf("qux", "quux", "corge"))

        val observer = TestObserver.create<Set<String>>()
        subject.unavailableUsernames.subscribe(observer)

        dispatcher.dispatch(ItemDetailAction.BeginCreateItemSession)
        listStub.onNext(list)

        dispatcher.dispatch(ItemDetailAction.EditField(hostname = "hostname1"))
        observer.assertLastValue(setOf("foo", "bar", "baz"))

        dispatcher.dispatch(ItemDetailAction.EditField(hostname = "hostname2"))
        observer.assertLastValue(setOf("qux", "quux", "corge"))
    }

    @Test
    fun `on edit, unavailableUsernames are calculated dynamically`() {
        val list =
            logins("hostname1", listOf("foo", "bar", "baz")) +
            logins("hostname2", listOf("qux", "quux", "corge"))

        val observer = TestObserver.create<Set<String>>()
        subject.unavailableUsernames.subscribe(observer)

        val original = list.first()
        dispatcher.dispatch(ItemDetailAction.BeginEditItemSession(original.id))
        listStub.onNext(list)
        getStub.onNext(original.asOptional())

        // we don't include the original (foo) because we would never be able to save it.
        observer.assertLastValue(setOf("bar", "baz"))
    }
}

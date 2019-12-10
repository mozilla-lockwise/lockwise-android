/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.rxkotlin.addTo
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import org.junit.Assert
import org.junit.Test

class ItemDetailStoreTest : DisposingTest() {
    val dispatcher = Dispatcher()
    val dispatcherObserver = createTestObserver<Action>()

    val subject = ItemDetailStore(dispatcher)

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

        dispatcher.dispatch(RouteAction.ItemDetail("id00000"))
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
        dispatcher.dispatch(RouteAction.EditItemDetail(itemId))
        observer.assertLastValue(true)

        dispatcher.dispatch(ItemDetailAction.EndEditing(itemId))
        observer.assertLastValue(false)
    }

    @Test
    fun `Save Changes is achieved by actions`() {
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

        val mutated = original.copy(password = "password1")

        dispatcher.dispatch(ItemDetailAction.SaveChanges(original, mutated))

        observer.assertLastValue(mutated)
    }
}

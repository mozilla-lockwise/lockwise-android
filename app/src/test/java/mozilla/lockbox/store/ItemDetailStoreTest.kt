/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.assertLastValue
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
            passwordVisible.onNext(true)
            isPasswordVisible.subscribe(observer)
        }

        dispatcher.dispatch(RouteAction.ItemList)
        observer.assertLastValue(false)
    }
    @Test
    fun `routing to ItemDetail resets state`() {
        val observer = createTestObserver<Boolean>()

        subject.apply {
            passwordVisible.onNext(true)
            isPasswordVisible.subscribe(observer)
        }

        dispatcher.dispatch(RouteAction.ItemDetail("id00000"))
        observer.assertLastValue(false)
    }

    @Test
    fun `toggles passwordVisible from action`() {
        val observer = createTestObserver<Boolean>()
        subject.isPasswordVisible.subscribe(observer)

        dispatcher.dispatch(ItemDetailAction.SetPasswordVisibility)
        observer.assertLastValue(true)

        dispatcher.dispatch(ItemDetailAction.SetPasswordVisibility)
        observer.assertLastValue(false)
    }
}
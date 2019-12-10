/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

class ItemDetailStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared = ItemDetailStore()
    }

    private val compositeDisposable = CompositeDisposable()

    internal val _passwordVisible = BehaviorSubject.createDefault(false)
    val isPasswordVisible: Observable<Boolean> = _passwordVisible

    internal val _isEditing = BehaviorSubject.createDefault(false)
    val isEditing: Observable<Boolean> = _isEditing

    init {
        dispatcher.register
            .filterByType(RouteAction::class.java)
            .subscribe { action ->
                when (action) {
                    is RouteAction.ItemDetail -> _passwordVisible.onNext(false)
                    is RouteAction.ItemList -> _passwordVisible.onNext(false)
                    is RouteAction.EditItemDetail -> startEditing()
                }
            }
            .addTo(compositeDisposable)

        dispatcher.register
            .filterByType(ItemDetailAction::class.java)
            .subscribe { action ->
                when (action) {
                    is ItemDetailAction.SetPasswordVisibility -> _passwordVisible.onNext(action.visible)
                    is ItemDetailAction.SaveChanges -> saveItem(action.previous, action.next)
                    is ItemDetailAction.EndEditing -> stopEditing()
                }
            }
            .addTo(compositeDisposable)
    }

    private fun startEditing() {
        _passwordVisible.onNext(false)
        _isEditing.onNext(true)
    }

    private fun saveItem(previous: ServerPassword, next: ServerPassword) {
        dispatcher.dispatch(DataStoreAction.UpdateItemDetail(previous, next))
        stopEditing()
    }

    private fun stopEditing() {
        _isEditing.onNext(false)
    }
}

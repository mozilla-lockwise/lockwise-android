/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.text.TextUtils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.ItemDetailAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.extensions.filter
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.pushError

@ExperimentalCoroutinesApi
class ItemDetailStore(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared = ItemDetailStore()
    }

    private val compositeDisposable = CompositeDisposable()

    private val sessionDisposable = CompositeDisposable()

    internal val _passwordVisible = BehaviorSubject.createDefault(false)
    val isPasswordVisible: Observable<Boolean> = _passwordVisible

    internal val _isEditing = BehaviorSubject.createDefault(false)
    val isEditing: Observable<Boolean> = _isEditing

    private val _originalItem = ReplaySubject.createWithSize<Optional<ServerPassword>>(1)
    val originalItem: Observable<Optional<ServerPassword>> = _originalItem

    private val _itemToSave = ReplaySubject.createWithSize<Optional<ServerPassword>>(1)
    val itemToSave: Observable<Optional<ServerPassword>> = _itemToSave

    private val _unavailableUsernames = BehaviorSubject.createDefault(emptySet<String>())
    val unavailableUsernames: Observable<Set<String>> = _unavailableUsernames

    private val _isDirty = BehaviorSubject.createDefault(false)
    val isDirty: Observable<Boolean> = _isDirty

    init {
        dispatcher.register
            .filterByType(RouteAction::class.java)
            .subscribe { action ->
                when (action) {
                    is RouteAction.DisplayItem -> _passwordVisible.onNext(false)
                    is RouteAction.ItemList -> _passwordVisible.onNext(false)
                }
            }
            .addTo(compositeDisposable)

        dispatcher.register
            .filterByType(ItemDetailAction::class.java)
            .subscribe { action ->
                when (action) {
                    is ItemDetailAction.SetPasswordVisibility -> _passwordVisible.onNext(action.visible)
                    is ItemDetailAction.EditField ->
                        updateCredentialField(action.hostname, action.username, action.hostname)

                    is ItemDetailAction.BeginEditItemSession -> startEditing(action.itemId)
                    is ItemDetailAction.BeginCreateItemSession -> startCreating()

                    is ItemDetailAction.EditItemSaveChanges -> saveEditChanges()
                    is ItemDetailAction.CreateItemSaveChanges -> saveCreateChanges()

                    is ItemDetailAction.EndCreateSession -> stopCreating()
                    is ItemDetailAction.EndEditSession -> stopEditing()
                }
            }
            .addTo(compositeDisposable)

        Observables.combineLatest(originalItem.filterNotNull(), itemToSave.filterNotNull())
            .map {
                it.first != it.second
            }
            .subscribe { _isDirty.onNext(it) }
            .addTo(compositeDisposable)
    }

    private fun updateCredentialField(hostname: String?, username: String?, password: String?) {
        val old = _itemToSave.value?.value ?: return pushError(
            NullPointerException("Credentials are null"),
            "Error editing credential"
        )
        val new = old.copy(
            hostname = hostname ?: old.hostname,
            username = if (!TextUtils.isEmpty(username)) username else old.username,
            password = password ?: old.password,

            formSubmitURL = if (!TextUtils.isEmpty(hostname)) hostname else old.formSubmitURL
        )

        _itemToSave.onNext(new.asOptional())
    }

    private fun startCreating() {
        _passwordVisible.onNext(false)
        _isEditing.onNext(true)

        calculateUnavailableUsernames(itemToSave, false)
    }

    private fun saveCreateChanges() {
        _itemToSave.value?.value?.let {
            dispatcher.dispatch(DataStoreAction.CreateItem(it))
        }
        stopCreating()
    }

    private fun stopCreating() {
        stopEditing()
    }

    private fun startEditing(itemId: String) {
        _passwordVisible.onNext(false)
        _isEditing.onNext(true)

        val getItem = dataStore.get(itemId)

        getItem
            .subscribe { _originalItem.onNext(it) }
            .addTo(sessionDisposable)

        _originalItem
            .map {
                it.value ?: ServerPassword(id = "", hostname = "", password = "")
            }
            .subscribe {
                _itemToSave.onNext(it.asOptional())
            }
            .addTo(sessionDisposable)

        calculateUnavailableUsernames(getItem, true)
    }

    private fun calculateUnavailableUsernames(getItem: Observable<Optional<ServerPassword>>, excludeItem: Boolean) {
        Observables.combineLatest(getItem, dataStore.list)
            .map { (opt, list) ->
                val item = opt.value ?: return@map emptySet<String>()

                val usernames = list
                    .filter(
                        hostname = item.hostname,
                        httpRealm = item.httpRealm,
                        formSubmitURL = item.formSubmitURL
                    )
                    .mapNotNull { it.username }
                    .toSet()
                    .minus("")

                item.username?.let {
                    if (excludeItem) {
                        usernames.minus(it)
                    } else {
                        null
                    }
                } ?: usernames
            }
            .subscribe {
                _unavailableUsernames.onNext(it)
            }
            .addTo(sessionDisposable)
    }

    private fun saveEditChanges() {
        val previous = _originalItem.value?.value ?: return
        val next = _itemToSave.value?.value ?: return
        dispatcher.dispatch(DataStoreAction.UpdateItemDetail(previous, next))
    }

    private fun stopEditing() {
        _isEditing.onNext(false)
        _originalItem.onNext(Optional(null))
        _originalItem.cleanupBuffer()

        sessionDisposable.clear()
    }
}

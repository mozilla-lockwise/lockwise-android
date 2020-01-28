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
import io.reactivex.subjects.PublishSubject
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

@ExperimentalCoroutinesApi
open class ItemDetailStore(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared by lazy { ItemDetailStore() }
    }

    private val compositeDisposable = CompositeDisposable()

    private val sessionDisposable = CompositeDisposable()

    internal val _passwordVisible = BehaviorSubject.createDefault(false)
    val isPasswordVisible: Observable<Boolean> = _passwordVisible

    internal val _isEditing = PublishSubject.create<Boolean>()
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

        originalItem
            .subscribe(_itemToSave::onNext)
            .addTo(compositeDisposable)

        dispatcher.register
            .filterByType(ItemDetailAction::class.java)
            .subscribe { action ->
                when (action) {
                    is ItemDetailAction.SetPasswordVisibility -> _passwordVisible.onNext(action.visible)
                    is ItemDetailAction.EditField ->
                        updateCredentialField(action.hostname, action.username, action.password)

                    is ItemDetailAction.BeginEditItemSession -> startEditing(action.itemId)
                    is ItemDetailAction.BeginCreateItemSession -> startCreating()

                    is ItemDetailAction.EditItemSaveChanges -> saveEditChanges()
                    is ItemDetailAction.CreateItemSaveChanges -> saveCreateChanges()

                    is ItemDetailAction.EndCreateItemSession -> stopCreating()
                    is ItemDetailAction.EndEditItemSession -> stopEditing()
                }
            }
            .addTo(compositeDisposable)

        Observables.combineLatest(originalItem.filterNotNull(), itemToSave.filterNotNull())
            .map {
                it.first != it.second
            }
            .subscribe(_isDirty::onNext)
            .addTo(compositeDisposable)
    }

    private fun updateCredentialField(hostname: String?, username: String?, password: String?) {
        itemToSave.take(1)
            .map { opt ->
                opt.value?.let { old ->
                    old.copy(
                        hostname = hostname ?: old.hostname,
                        username = username ?: old.username,
                        password = password ?: old.password,

                        // This is only used in the create flow, so we're not
                        // going to be overwriting a formSubmitURL captured
                        // from the web.
                        formSubmitURL = if (!TextUtils.isEmpty(hostname)) hostname else old.formSubmitURL
                    )
                }.asOptional()
            }
            .subscribe(_itemToSave::onNext)
            .addTo(sessionDisposable)
    }

    private val emptyCredentials = ServerPassword(id = "", hostname = "", password = "")

    private fun startCreating() {
        _passwordVisible.onNext(false)
        _isEditing.onNext(true)
        _originalItem.onNext(emptyCredentials.asOptional())
        calculateUnavailableUsernames(itemToSave, false)
    }

    private fun saveCreateChanges() {
        itemToSave.take(1)
            .filterNotNull()
            .map { DataStoreAction.CreateItem(it) }
            .doAfterNext { stopCreating() }
            .subscribe(dispatcher::dispatch)
            .addTo(sessionDisposable)
    }

    private fun stopCreating() {
        stopEditing()
    }

    private fun startEditing(itemId: String) {
        _passwordVisible.onNext(false)
        _isEditing.onNext(true)

        dataStore.get(itemId).take(1)
            .subscribe(_originalItem::onNext)
            .addTo(sessionDisposable)

        calculateUnavailableUsernames(originalItem, true)
    }

    private fun calculateUnavailableUsernames(getItem: Observable<Optional<ServerPassword>>, excludeItem: Boolean) {
        Observables.combineLatest(getItem, dataStore.list.take(1))
            .map { (opt, list) ->
                val item = opt.value ?: return@map emptySet<String>()

                val usernames = list
                    .filter(
                        // Default to a blank space for Create, otherwise we get all usernames for any hostname.
                        hostname = if (item.hostname.isNullOrBlank()) " " else item.hostname,
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
        Observables.combineLatest(
                originalItem.take(1).filterNotNull(),
                itemToSave.take(1).filterNotNull()
            )
            .map { (previous, next) ->
                DataStoreAction.UpdateItemDetail(previous, next)
            }
            .doAfterNext { stopEditing() }
            .subscribe(dispatcher::dispatch)
            .addTo(sessionDisposable)
    }

    private fun stopEditing() {
        _isEditing.onNext(false)
        _originalItem.onNext(null.asOptional())
        sessionDisposable.clear()
    }
}

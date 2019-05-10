/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.concept.sync.Avatar
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.support.asOptional

interface AccountSettingView {
    fun setDisplayName(text: String)
    fun setAvatar(avatar: Avatar)
    val disconnectButtonClicks: Observable<Unit>
}

@ExperimentalCoroutinesApi
class AccountSettingPresenter(
    val view: AccountSettingView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val accountStore: AccountStore = AccountStore.shared
) : Presenter() {

    override fun onViewReady() {
        accountStore.profile
            .map {
                (it.value?.displayName ?: it.value?.email).asOptional()
            }
            .filterNotNull()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::setDisplayName)
            .addTo(compositeDisposable)

        accountStore.profile
            .map {
                it.value?.avatar.asOptional()
            }
            .filterNotNull()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::setAvatar)
            .addTo(compositeDisposable)

        view.disconnectButtonClicks
            .subscribe {
                dispatcher.dispatch(DialogAction.UnlinkDisclaimer)
            }
            .addTo(compositeDisposable)
    }
}
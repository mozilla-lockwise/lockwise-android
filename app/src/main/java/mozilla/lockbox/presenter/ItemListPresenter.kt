/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.action.ToastNotificationAction
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.extensions.mapToItemViewModelList
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.model.AccountViewModel
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.model.ToastNotificationViewModel
import mozilla.lockbox.model.titleFromHostname
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.store.SettingStore

interface ItemListView {
    val itemSelection: Observable<ItemViewModel>
    val filterClicks: Observable<Unit>
    val noEntriesClicks: Observable<Unit>
    val menuItemSelections: Observable<Int>
    val lockNowClick: Observable<Unit>
    val createNewEntryClick: Observable<Unit>
    val sortItemSelection: Observable<Setting.ItemListSort>
    fun updateItems(itemList: List<ItemViewModel>)
    fun updateAccountProfile(profile: AccountViewModel)
    fun updateItemListSort(sort: Setting.ItemListSort)
    fun loading(isLoading: Boolean)
    fun handleNetworkError(networkErrorVisibility: Boolean)
//    val retryNetworkConnectionClicks: Observable<Unit>
    val refreshItemList: Observable<Unit>
    val isRefreshing: Boolean
    fun stopRefreshing()
}

@ExperimentalCoroutinesApi
class ItemListPresenter(
    private val view: ItemListView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared,
    private val networkStore: NetworkStore = NetworkStore.shared,
    private val accountStore: AccountStore = AccountStore.shared
) : Presenter() {

    override fun onViewReady() {
        dataStore.syncState
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                when (it) {
                    DataStore.SyncState.Syncing -> true
                    DataStore.SyncState.NotSyncing -> false
                }
            }
            .subscribe { syncing ->
                when (view.isRefreshing) {
                    true -> if (!syncing) {
                        view.stopRefreshing()
                    }
                    false -> view.loading(syncing)
                }
            }
            .addTo(compositeDisposable)

        Observables.combineLatest(dataStore.list, settingStore.itemListSortOrder)
            .distinctUntilChanged()
            .map { pair ->
                when (pair.second) {
                    Setting.ItemListSort.ALPHABETICALLY -> pair.first.sortedBy { titleFromHostname(it.hostname) }
                    Setting.ItemListSort.RECENTLY_USED -> pair.first.sortedBy { -it.timeLastUsed }
                }
            }
            .mapToItemViewModelList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::updateItems)
            .addTo(compositeDisposable)

        settingStore.itemListSortOrder
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::updateItemListSort)
            .addTo(compositeDisposable)

        view.itemSelection
            .map { RouteAction.DisplayItem(it.id) }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.filterClicks
            .map { RouteAction.Filter }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.noEntriesClicks
            .map { AppWebPageAction.FaqSync }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.menuItemSelections
            .subscribe(this::onMenuItem)
            .addTo(compositeDisposable)

        view.lockNowClick
            .map {
                if (fingerprintStore.isDeviceSecure)
                    DataStoreAction.Lock
                else DialogAction.SecurityDisclaimer
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.createNewEntryClick
            .map { RouteAction.CreateItem }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.sortItemSelection
            .map { sortBy ->
                SettingAction.ItemListSortOrder(sortBy)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        view.refreshItemList
            .doOnDispose {
                view.stopRefreshing()
            }
            .subscribe {
                dispatcher.dispatch(DataStoreAction.Sync)
            }
            .addTo(compositeDisposable)

        accountStore.profile
            .filterNotNull()
            .map {
                AccountViewModel(
                    accountName = it.displayName,
                    displayEmailName = it.email,
                    avatarFromURL = it.avatar?.url
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(view::updateAccountProfile)
            .addTo(compositeDisposable)

        networkStore.isConnected
            .subscribe(view::handleNetworkError)
            .addTo(compositeDisposable)

        dataStore.deletedItem
            .map {
                val event = it.get()
                ToastNotificationAction.ShowDeleteToast(event?.formSubmitURL ?: event?.hostname)
            }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        // TODO: make this more robust to retry loading the correct page again (loadUrl)
//        view.retryNetworkConnectionClicks.subscribe {
//            dispatcher.dispatch(NetworkAction.CheckConnectivity)
//        }?.addTo(compositeDisposable)
    }

    private fun onMenuItem(@IdRes item: Int) {
        val action = when (item) {
            R.id.setting_menu_item -> RouteAction.SettingList
            R.id.account_setting_menu_item -> RouteAction.AccountSetting
            R.id.faq_menu_item -> AppWebPageAction.FaqList
            R.id.feedback_menu_item -> AppWebPageAction.SendFeedback
            else -> return log.error("Cannot route from item list menu")
        }
        dispatcher.dispatch(action)
    }
}
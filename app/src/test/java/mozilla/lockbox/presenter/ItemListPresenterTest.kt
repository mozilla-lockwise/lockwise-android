/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import android.net.ConnectivityManager
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.components.concept.sync.Profile
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.action.ToastNotificationAction
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.toViewModel
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.AccountViewModel
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.NetworkStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.Consumable
import mozilla.lockbox.support.Optional
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
open class ItemListPresenterTest {

    private val item1 = ServerPasswordTestHelper().item1
    private val item2 = ServerPasswordTestHelper().item2
    private val item3 = ServerPasswordTestHelper().item3

    class FakeView : ItemListView {

//        private val retryButtonStub = PublishSubject.create<Unit>()
//        override val retryNetworkConnectionClicks: Observable<Unit>
//            get() = retryButtonStub

        var networkAvailable = PublishSubject.create<Boolean>()
        override fun handleNetworkError(networkErrorVisibility: Boolean) {
            networkAvailable.onNext(networkErrorVisibility)
        }

        var accountViewModel: AccountViewModel? = null
        var updateItemsArgument: List<ItemViewModel>? = null
        var itemListSort: Setting.ItemListSort? = null
        var isLoading: Boolean? = null

        var toastNotificationArgStrId: Int? = null
        var toastNotificationArgText: String? = null

        val menuItemSelectionStub = PublishSubject.create<Int>()
        val itemSelectedStub = PublishSubject.create<ItemViewModel>()
        val filterClickStub = PublishSubject.create<Unit>()
        val noEntriesClickStub = PublishSubject.create<Unit>()
        val createNewEntryClickStub = PublishSubject.create<Unit>()
        val sortItemSelectionStub = PublishSubject.create<Setting.ItemListSort>()
        val disclaimerActionStub = PublishSubject.create<AlertState>()
        val lockNowSelectionStub = PublishSubject.create<Unit>()
        val refreshItemListStub = PublishSubject.create<Unit>()

        override val itemSelection: Observable<ItemViewModel>
            get() = itemSelectedStub

        override val sortItemSelection: Observable<Setting.ItemListSort>
            get() = sortItemSelectionStub

        override val filterClicks: Observable<Unit>
            get() = filterClickStub

        override val menuItemSelections: Observable<Int>
            get() = menuItemSelectionStub

        override val lockNowClick: Observable<Unit>
            get() = lockNowSelectionStub

        override val createNewEntryClick: Observable<Unit>
            get() = createNewEntryClickStub

        override val noEntriesClicks: Observable<Unit>
            get() = noEntriesClickStub

        override fun updateAccountProfile(profile: AccountViewModel) {
            accountViewModel = AccountViewModel(
                profile.accountName,
                profile.displayEmailName,
                profile.avatarFromURL
            )
        }

        override fun updateItems(itemList: List<ItemViewModel>) {
            updateItemsArgument = itemList
        }

        override fun updateItemListSort(sort: Setting.ItemListSort) {
            itemListSort = sort
        }

        override fun loading(isLoading: Boolean) {
            this.isLoading = isLoading
        }

        override val refreshItemList: Observable<Unit>
            get() = refreshItemListStub

        override val isRefreshing: Boolean = false

        override fun stopRefreshing() {}
    }

    val listStub = PublishSubject.create<List<ServerPassword>>()
    val syncStateStub = PublishSubject.create<DataStore.SyncState>()
    val itemListSortStub = BehaviorSubject.createDefault(Setting.ItemListSort.ALPHABETICALLY)

    @Mock
    val settingStore = PowerMockito.mock(SettingStore::class.java)!!

    @Mock
    val fingerprintStore = PowerMockito.mock(FingerprintStore::class.java)!!

    @Mock
    val accountStore = PowerMockito.mock(AccountStore::class.java)!!

    @Mock
    val networkStore = PowerMockito.mock(NetworkStore::class.java)!!

    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!

    @Mock
    private val connectivityManager = PowerMockito.mock(ConnectivityManager::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    private var isConnected: Observable<Boolean> = PublishSubject.create()
    private var isConnectedObserver: TestObserver<Boolean> = TestObserver.create<Boolean>()
    private val profileStub = PublishSubject.create<Optional<Profile>>()
    private var deleteItemSubjectStub = PublishSubject.create<Consumable<ServerPassword>>()

    val view: FakeView = spy(FakeView())
    val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()!!

    lateinit var subject: ItemListPresenter

    @Before
    fun setUp() {
        whenCalled(networkStore.isConnected).thenReturn(isConnected)

        PowerMockito.`when`(accountStore.profile).thenReturn(profileStub)
        PowerMockito.`when`(settingStore.itemListSortOrder).thenReturn(itemListSortStub)
        PowerMockito.`when`(dataStore.list).thenReturn(listStub)
        PowerMockito.`when`(dataStore.syncState).thenReturn(syncStateStub)
        PowerMockito.`when`(dataStore.deletedItem).thenReturn(deleteItemSubjectStub)

        PowerMockito.whenNew(AccountStore::class.java).withAnyArguments().thenReturn(accountStore)
        PowerMockito.whenNew(SettingStore::class.java).withAnyArguments().thenReturn(settingStore)
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments().thenReturn(dataStore)
        dispatcher.register.subscribe(dispatcherObserver)

        networkStore.connectivityManager = connectivityManager
        view.networkAvailable.subscribe(isConnectedObserver)

        subject = ItemListPresenter(
            view,
            dispatcher,
            dataStore,
            settingStore,
            fingerprintStore,
            networkStore,
            accountStore
        )

        subject.onViewReady()
    }

    @Test
    fun accountViewModelMapping() {
        val profile = AccountViewModel(
            accountName = "DogLover Jones",
            displayEmailName = "ilovedogs@dogs.com",
            avatarFromURL = null
        )
        view.updateAccountProfile(profile)
        Assert.assertEquals(profile.accountName, view.accountViewModel!!.accountName)
        Assert.assertEquals(profile.displayEmailName, view.accountViewModel!!.displayEmailName)
        Assert.assertEquals(profile.avatarFromURL, view.accountViewModel!!.avatarFromURL)
    }

    @Test
    fun receivingItemSelected() {
        val id = "the_guid"
        view.itemSelectedStub.onNext(ItemViewModel("title", "subtitle", id))

        dispatcherObserver.assertLastValue(RouteAction.DisplayItem(id))
    }

    @Test
    fun receivingPasswordList_somePasswords() {
        val list = listOf(item1, item2, item3)
        val expectedList = listOf(item2, item3, item1).map { it.toViewModel() }

        listStub.onNext(list)

        Assert.assertEquals(expectedList, view.updateItemsArgument)
        Assert.assertEquals(Setting.ItemListSort.ALPHABETICALLY, view.itemListSort)
    }

    @Test
    fun `updates sort order of list when item sort order menu changes`() {
        val list = listOf(item1, item2, item3)
        listStub.onNext(list)
        val alphabetically = listOf(item2, item3, item1).map { it.toViewModel() }
        val lastUsed = listOf(item3, item2, item1).map { it.toViewModel() }

        // default
        Assert.assertEquals(alphabetically, view.updateItemsArgument)
        Assert.assertEquals(Setting.ItemListSort.ALPHABETICALLY, view.itemListSort)

        // last used
        var sortOrder = Setting.ItemListSort.RECENTLY_USED
        view.sortItemSelectionStub.onNext(sortOrder)
        dispatcherObserver.assertLastValue(SettingAction.ItemListSortOrder(sortOrder))

        itemListSortStub.onNext(sortOrder)
        Assert.assertEquals(sortOrder, view.itemListSort)
        Assert.assertEquals(lastUsed, view.updateItemsArgument)

        // alphabetically
        sortOrder = Setting.ItemListSort.ALPHABETICALLY
        view.sortItemSelectionStub.onNext(sortOrder)
        dispatcherObserver.assertLastValue(SettingAction.ItemListSortOrder(sortOrder))

        itemListSortStub.onNext(sortOrder)
        Assert.assertEquals(sortOrder, view.itemListSort)
        Assert.assertEquals(alphabetically, view.updateItemsArgument)
    }

    @Test
    fun receivingPasswordList_empty() {
        listStub.onNext(emptyList())
        Assert.assertEquals(emptyList<ItemViewModel>(), view.updateItemsArgument)
    }

    @Test
    fun `menuItem clicks cause RouteActions`() {
        view.menuItemSelectionStub.onNext(R.id.setting_menu_item)
        dispatcherObserver.assertLastValue(RouteAction.SettingList)
        view.menuItemSelectionStub.onNext(R.id.account_setting_menu_item)
        dispatcherObserver.assertLastValue(RouteAction.AccountSetting)
        view.menuItemSelectionStub.onNext(R.id.faq_menu_item)
        dispatcherObserver.assertLastValue(AppWebPageAction.FaqList)
        view.menuItemSelectionStub.onNext(R.id.feedback_menu_item)
        dispatcherObserver.assertLastValue(AppWebPageAction.SendFeedback)
    }

    @Test
    fun `filter clicks cause RouteAction Filter`() {
        view.filterClickStub.onNext(Unit)
        Assert.assertTrue(dispatcherObserver.values().last() is RouteAction.Filter)
    }

    @Test
    fun `click on create button takes you to manual create view`() {
        view.createNewEntryClickStub.onNext(Unit)
        Assert.assertTrue(dispatcherObserver.values().last() is RouteAction.CreateItem)
    }

    @Test
    fun `no matching entries clicks routes to FAQ`() {
        view.noEntriesClickStub.onNext(Unit)
        dispatcherObserver.assertLastValue(AppWebPageAction.FaqSync)
    }

    @Test
    fun `tapping on the lock menu item when the user has no device security routes to security disclaimer dialog`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(false)
        setUp()
        view.lockNowSelectionStub.onNext(Unit)
        view.disclaimerActionStub.onNext(AlertState.BUTTON_POSITIVE)

        Assert.assertTrue(dispatcherObserver.values().last() is DialogAction.SecurityDisclaimer)
    }

    @Test
    fun `tapping on the lock menu item when the user has device security locks the datastore`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(true)
        view.lockNowSelectionStub.onNext(Unit)
        dispatcherObserver.assertLastValue(DataStoreAction.Lock)
    }

    @Test
    fun `show sync loading indicator`() {
        syncStateStub.onNext(DataStore.SyncState.Syncing)
        Assert.assertEquals(true, view.isLoading)
    }

    @Test
    fun `remove sync loading indicator`() {
        syncStateStub.onNext(DataStore.SyncState.NotSyncing)
        Assert.assertEquals(false, view.isLoading)
    }

    @Test
    fun `item deleted toast`() {
        val item = ServerPasswordTestHelper().item1
        val hostname = item.hostname
        deleteItemSubjectStub.onNext(Consumable(item))
        dispatcherObserver.assertLastValue(ToastNotificationAction.ShowDeleteToast(hostname))
    }

    @Test
    fun `stop refreshing when stop syncing after pull to refresh`() {
        whenCalled(view.isRefreshing).thenReturn(true)
        syncStateStub.onNext(DataStore.SyncState.NotSyncing)
        verify(view).stopRefreshing()
    }

    @Test
    fun `swipe down calls sync`() {
        view.refreshItemListStub.onNext(Unit)
        dispatcherObserver.assertLastValue(DataStoreAction.Sync)
    }

    @Test
    fun `network error visibility is correctly being set`() {
        val value = view.networkAvailable
        value.onNext(true)

        isConnectedObserver.assertValue(true)
    }
}

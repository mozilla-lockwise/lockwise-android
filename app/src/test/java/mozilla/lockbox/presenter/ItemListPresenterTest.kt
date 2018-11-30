/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
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
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.support.FxAProfile
import mozilla.lockbox.support.Optional
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.mockito.Mockito.`when` as whenCalled

private val username = "dogs@dogs.com"
private val password1 = ServerPassword(
    "fdsfda",
    "https://www.mozilla.org",
    username,
    "woof",
    timesUsed = 0,
    timeCreated = 0L,
    timeLastUsed = 1L,
    timePasswordChanged = 0L
)
private val password2 = ServerPassword(
    "ghfdhg",
    "https://www.cats.org",
    username,
    "meow",
    timesUsed = 0,
    timeCreated = 0L,
    timeLastUsed = 2L,
    timePasswordChanged = 0L
)
private val password3 = ServerPassword(
    "ioupiouiuy",
    "www.dogs.org",
    username = "",
    password = "baaaaa",
    timesUsed = 0,
    timeCreated = 0L,
    timeLastUsed = 3L,
    timePasswordChanged = 0L
)

@RunWith(RobolectricTestRunner::class)
@PrepareForTest(AccountStore::class)
open class ItemListPresenterTest {
    @Mock
    val fingerprintStore = PowerMockito.mock(FingerprintStore::class.java)

    @Mock
    val accountStore = PowerMockito.mock(AccountStore::class.java)

    class FakeView : ItemListView {

        var setDisplayNameArgument: String? = null
        var updateItemsArgument: List<ItemViewModel>? = null
        var itemListSort: Setting.ItemListSort? = null
        val menuItemSelectionStub = PublishSubject.create<Int>()
        val itemSelectedStub = PublishSubject.create<ItemViewModel>()
        val filterClickStub = PublishSubject.create<Unit>()
        val sortItemSelectionStub = PublishSubject.create<Setting.ItemListSort>()
        val disclaimerActionStub = PublishSubject.create<AlertState>()
        val lockNowSelectionStub = PublishSubject.create<Unit>()

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

        override fun updateAccountProfile(profile: AccountViewModel) {
            setDisplayNameArgument = profile.displayEmailName
        }

        override fun updateItems(itemList: List<ItemViewModel>) {
            updateItemsArgument = itemList
        }

        override fun updateItemListSort(sort: Setting.ItemListSort) {
            itemListSort = sort
        }
    }

    class FakeDataStore : DataStore() {

        val listStub = PublishSubject.create<List<ServerPassword>>()
        override val list: Observable<List<ServerPassword>>
            get() = listStub
    }

    class FakeSettingStore : SettingStore() {

        val itemListSortStub = BehaviorSubject.createDefault(Setting.ItemListSort.ALPHABETICALLY)
        override var itemListSortOrder: Observable<Setting.ItemListSort> = itemListSortStub
    }

    private val dataStore = FakeDataStore()
    private val settingStore = FakeSettingStore()
    private val profileStub = PublishSubject.create<Optional<FxAProfile>>()

    val view = FakeView()
    val dispatcher = Dispatcher()
    val subject = ItemListPresenter(view, dispatcher, dataStore, settingStore, fingerprintStore, accountStore)

    private val dispatcherObserver = TestObserver.create<Action>()!!

    @Before
    fun setUp() {

        PowerMockito.`when`(accountStore.profile).thenReturn(profileStub)
        PowerMockito.whenNew(AccountStore::class.java).withAnyArguments().thenReturn(accountStore)
        dispatcher.register.subscribe(dispatcherObserver)

        subject.onViewReady()
    }

    @Test
    fun receivingItemSelected() {
        val id = "the_guid"
        view.itemSelectedStub.onNext(ItemViewModel("title", "subtitle", id))

        dispatcherObserver.assertLastValue(RouteAction.ItemDetail(id))
    }

    @Test
    fun receivingPasswordList_somePasswords() {
        val list = listOf(password1, password2, password3)
        dataStore.listStub.onNext(list)
        val expectedList = listOf(password2, password3, password1).map { it.toViewModel() }

        Assert.assertEquals(expectedList, view.updateItemsArgument)
        Assert.assertEquals(Setting.ItemListSort.ALPHABETICALLY, view.itemListSort)
    }

    @Test
    fun `updates sort order of list when item sort order menu changes`() {
        val list = listOf(password1, password2, password3)
        dataStore.listStub.onNext(list)
        val alphabetically = listOf(password2, password3, password1).map { it.toViewModel() }
        val lastUsed = listOf(password3, password2, password1).map { it.toViewModel() }

        // default
        Assert.assertEquals(alphabetically, view.updateItemsArgument)
        Assert.assertEquals(Setting.ItemListSort.ALPHABETICALLY, view.itemListSort)

        // last used
        var sortOrder = Setting.ItemListSort.RECENTLY_USED
        view.sortItemSelectionStub.onNext(sortOrder)
        dispatcherObserver.assertLastValue(SettingAction.ItemListSortOrder(sortOrder))

        settingStore.itemListSortStub.onNext(sortOrder)
        Assert.assertEquals(sortOrder, view.itemListSort)
        Assert.assertEquals(lastUsed, view.updateItemsArgument)

        // alphabetically
        sortOrder = Setting.ItemListSort.ALPHABETICALLY
        view.sortItemSelectionStub.onNext(sortOrder)
        dispatcherObserver.assertLastValue(SettingAction.ItemListSortOrder(sortOrder))

        settingStore.itemListSortStub.onNext(sortOrder)
        Assert.assertEquals(sortOrder, view.itemListSort)
        Assert.assertEquals(alphabetically, view.updateItemsArgument)
    }

    @Test
    fun receivingPasswordList_empty() {
        dataStore.listStub.onNext(emptyList())

        Assert.assertNull(view.updateItemsArgument)
    }

    @Test
    fun `menuItem clicks cause RouteActions`() {
        view.menuItemSelectionStub.onNext(R.id.setting_menu_item)
        dispatcherObserver.assertLastValue(RouteAction.SettingList)
        view.menuItemSelectionStub.onNext(R.id.account_setting_menu_item)
        dispatcherObserver.assertLastValue(RouteAction.AccountSetting)
    }

    @Test
    fun `tapping on the lock menu item when the user has no device security routes to security disclaimer dialog`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(false)
        setUp()
        view.lockNowSelectionStub.onNext(Unit)
        view.disclaimerActionStub.onNext(AlertState.BUTTON_POSITIVE)

        Assert.assertTrue(dispatcherObserver.values().last() is RouteAction.Dialog.SecurityDisclaimer)
    }

    @Test
    fun `tapping on the lock menu item when the user has device security routes to lock screen`() {
        whenCalled(fingerprintStore.isDeviceSecure).thenReturn(true)
        view.lockNowSelectionStub.onNext(Unit)
        dispatcherObserver.assertLastValue(RouteAction.LockScreen)
    }
}

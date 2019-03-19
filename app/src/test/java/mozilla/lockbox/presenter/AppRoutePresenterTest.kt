/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.list_cell_no_entries.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.AutofillFilterFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import mozilla.lockbox.view.ItemListFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Navigation::class,
    Intent::class
)
class AppRoutePresenterTest {

    @Mock
    val navController: NavController = Mockito.mock(NavController::class.java)

    @Mock
    val navDestination: NavDestination = Mockito.mock(NavDestination::class.java)


    @Mock
    val itemListFragment: ItemListFragment = PowerMockito.mock(ItemListFragment::class.java)

    @Mock
    val fragmentManager: FragmentManager = Mockito.mock(FragmentManager::class.java)

    @Mock
    val activity: AppCompatActivity = Mockito.mock(AppCompatActivity::class.java)

    class FakeRouteStore : RouteStore() {
        val routeStub = PublishSubject.create<RouteAction>()
        override val routes: Observable<RouteAction>
            get() = routeStub
    }

    class FakeSettingStore : SettingStore() {
        val itemListSortStub = BehaviorSubject.createDefault(Setting.ItemListSort.ALPHABETICALLY)
        override var itemListSortOrder: Observable<Setting.ItemListSort> = itemListSortStub
    }

    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val routeStore = FakeRouteStore()
    private val settingStore = FakeSettingStore()

    var desinationIdStub = R.id.welcome_fragment

    lateinit var subject: AppRoutePresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        PowerMockito.`when`(activity.supportFragmentManager).thenReturn(fragmentManager)
        PowerMockito.`when`(navDestination.id).thenReturn(desinationIdStub)
        PowerMockito.`when`(navController.currentDestination).thenReturn(navDestination)
        PowerMockito.mockStatic(Navigation::class.java)
        PowerMockito.`when`(Navigation.findNavController(activity, R.id.fragment_nav_host)).thenReturn(navController)

        PowerMockito.whenNew(ItemListFragment::class.java).withNoArguments()
            .thenReturn(itemListFragment)

        subject = AppRoutePresenter(
            activity,
            dispatcher,
            routeStore,
            settingStore
        )
    }

    // TODO
    @Test
    fun `routes navigate correctly`() {
        desinationIdStub = R.id.action_to_itemList
        routeStore.routeStub.onNext(RouteAction.ItemList)
        subject.onViewReady()

        verify(navController).navigate(R.id.action_to_itemList)
    }

}

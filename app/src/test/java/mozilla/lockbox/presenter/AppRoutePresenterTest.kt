/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Navigation::class,
    FingerprintAuthDialogFragment::class,
    Intent::class
)
class AppRoutePresenterTest {

    @Mock
    val activity: AppCompatActivity = Mockito.mock(AppCompatActivity::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    val navController: NavController = Mockito.mock(NavController::class.java)

    @Mock
    val navDestination: NavDestination = Mockito.mock(NavDestination::class.java)

    @Mock
    private val settingStore = Mockito.mock(SettingStore::class.java)

    class FakeRouteStore : RouteStore() {
        val routeStub = PublishSubject.create<RouteAction>()
        override val routes: Observable<RouteAction>
            get() = routeStub
    }

    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val routeStore = FakeRouteStore()

    lateinit var subject: AppRoutePresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)
        PowerMockito.`when`(navDestination.id).thenReturn(R.id.fragment_null)
        PowerMockito.`when`(navController.currentDestination).thenReturn(navDestination)

        PowerMockito.mockStatic(Navigation::class.java)

        subject = AppRoutePresenter(
            activity,
            dispatcher,
            routeStore,
            settingStore
        )
        subject.navController = navController
    }

    @Test
    fun `web view bundle is created`() {
        val action = AppWebPageAction.FaqList
        val result = subject.bundle(action)
        Assert.assertThat(result, CoreMatchers.instanceOf(Bundle::class.java))
    }

    @Test
    fun `item detail bundle is created`() {
        val action = RouteAction.ItemDetail("id")
        val result = subject.bundle(action)
        Assert.assertThat(result, CoreMatchers.instanceOf(Bundle::class.java))
    }
}
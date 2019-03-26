/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.AutofillFilterFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
    AutofillRoutePresenter::class,
    Navigation::class,
    FingerprintAuthDialogFragment::class,
    AutofillFilterFragment::class,
    Intent::class
)
class RoutePresenterTest {

    @Mock
    val activity: AppCompatActivity = Mockito.mock(AppCompatActivity::class.java)

    @Mock
    val navController: NavController = Mockito.mock(NavController::class.java)

    @Mock
    val fragmentManager: FragmentManager = Mockito.mock(FragmentManager::class.java)

    @Mock
    val navDestination: NavDestination = Mockito.mock(NavDestination::class.java)

    class FakeRouteStore : RouteStore() {
        val routeStub = PublishSubject.create<RouteAction>()
        override val routes: Observable<RouteAction>
            get() = routeStub
    }

    private val immediate = object : Scheduler() {
        override fun scheduleDirect(
            run: Runnable,
            delay: Long,
            unit: TimeUnit
        ): Disposable {
            return super.scheduleDirect(run, 0, unit)
        }

        override fun createWorker(): Scheduler.Worker {
            return ExecutorScheduler.ExecutorWorker(
                Executor { it.run() })
        }
    }

    private val dispatcher = Dispatcher()
    private val dispatcherObserver = TestObserver.create<Action>()
    private val routeStore = FakeRouteStore()

    lateinit var subject: RoutePresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

        // needed to make `.observeOn(AndroidSchedulers.mainThread())` work
        // this is unnecessary in other tests because we are using the
        // robolectric testrunner there.
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediate }
        whenCalled(activity.supportFragmentManager).thenReturn(fragmentManager)
        whenCalled(navDestination.id).thenReturn(R.id.fragment_null)
        whenCalled(navController.currentDestination).thenReturn(navDestination)
        PowerMockito.mockStatic(Navigation::class.java)
        whenCalled(navController).thenReturn(Navigation.findNavController(activity, R.id.fragment_nav_host))

        subject = RoutePresenter(
            activity,
            dispatcher,
            routeStore
        )
        subject.navController = navController
//        subject.onViewReady()
    }
}
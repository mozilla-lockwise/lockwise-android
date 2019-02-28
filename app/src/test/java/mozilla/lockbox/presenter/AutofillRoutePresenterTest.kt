/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.view.AutofillFilterFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
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
    AutofillFilterFragment::class
)
class AutofillRoutePresenterTest {
    @Mock
    val navController: NavController = mock(NavController::class.java)

    @Mock
    val navDestination: NavDestination = mock(NavDestination::class.java)

    @Mock
    val fingerprintAuthDialogFragment: FingerprintAuthDialogFragment = PowerMockito.mock(FingerprintAuthDialogFragment::class.java)

    @Mock
    val autofillFilterFragment: AutofillFilterFragment = PowerMockito.mock(AutofillFilterFragment::class.java)

    @Mock
    val fragmentManager: FragmentManager = mock(FragmentManager::class.java)

    @Mock
    val activity: AppCompatActivity = mock(AppCompatActivity::class.java)

    @Mock
    val responseBuilder: FillResponseBuilder = mock(FillResponseBuilder::class.java)

//    @Mock
//    val filteredFillResponse: FillResponse = mock(FillResponse::class.java)
//
//    @Mock
//    val fallbackFillResponse: FillResponse = mock(FillResponse::class.java)

    class FakeRouteStore : RouteStore() {
        val routeStub = PublishSubject.create<RouteAction>()
        override val routes: Observable<RouteAction>
            get() = routeStub
    }

    class FakeDataStore : DataStore() {
        val stateStub = PublishSubject.create<DataStore.State>()
        override val state: Observable<State>
            get() = stateStub
    }

    class FakePslSupport : PublicSuffixSupport()

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
    private val routeStore = FakeRouteStore()
    private val dataStore = FakeDataStore()
    private val pslSupport = FakePslSupport()
    private val dispatcherObserver = TestObserver.create<Action>()

    lateinit var subject: AutofillRoutePresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

        // needed to make `.observeOn(AndroidSchedulers.mainThread())` work
        // this is unnecessary in other tests because we are using the
        // robolectric testrunner there.
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediate }

//        whenCalled(responseBuilder.buildFilteredFillResponse(eq(activity), anyList())).thenReturn(filteredFillResponse)
//        whenCalled(responseBuilder.buildFallbackFillResponse(eq(activity))).thenReturn(fallbackFillResponse)

        PowerMockito.whenNew(FingerprintAuthDialogFragment::class.java).withNoArguments()
            .thenReturn(fingerprintAuthDialogFragment)
        PowerMockito.whenNew(AutofillFilterFragment::class.java).withNoArguments()
            .thenReturn(autofillFilterFragment)

        whenCalled(activity.supportFragmentManager).thenReturn(fragmentManager)
        whenCalled(navDestination.id).thenReturn(R.id.fragment_null)
        whenCalled(navController.currentDestination).thenReturn(navDestination)
        PowerMockito.mockStatic(Navigation::class.java)
        whenCalled(Navigation.findNavController(activity, R.id.autofill_fragment_nav_host)).thenReturn(navController)

        subject = AutofillRoutePresenter(activity, responseBuilder, dispatcher, routeStore, dataStore, pslSupport)
        subject.onViewReady()
    }

    @Test
    fun `locked routes`() {
        routeStore.routeStub.onNext(RouteAction.LockScreen)

        verify(navController).navigate(R.id.action_to_locked, null)
    }

    @Test
    fun `item list routes navigate to search`() {
        routeStore.routeStub.onNext(RouteAction.ItemList)

        verify(autofillFilterFragment).show(eq(fragmentManager), anyString())
        verify(autofillFilterFragment).setupDialog(R.string.autofill, null)
    }

    @Test
    fun `fingerprint dialog routes navigate to fingerprint dialog`() {
        val title = R.string.fingerprint_dialog_title
        routeStore.routeStub.onNext(RouteAction.DialogFragment.FingerprintDialog(title))

        verify(fingerprintAuthDialogFragment).show(eq(fragmentManager), anyString())
        verify(fingerprintAuthDialogFragment).setupDialog(title, null)
    }
}
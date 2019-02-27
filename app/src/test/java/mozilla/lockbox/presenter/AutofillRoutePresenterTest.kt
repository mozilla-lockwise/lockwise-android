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
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(Navigation::class, FingerprintAuthDialogFragment::class)
class AutofillRoutePresenterTest {
    @Mock
    val navController: NavController = mock(NavController::class.java)

    @Mock
    val navDestination: NavDestination = mock(NavDestination::class.java)

    @Mock
    val fingerprintAuthDialogFragment: FingerprintAuthDialogFragment = mock(FingerprintAuthDialogFragment::class.java)

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

    private val dispatcher = Dispatcher()
    private val routeStore = FakeRouteStore()
    private val dataStore = FakeDataStore()
    private val pslSupport = FakePslSupport()
    private val dispatcherObserver = TestObserver.create<Action>()

    lateinit var subject: AutofillRoutePresenter

    @Before
    fun setUp() {
        dispatcher.register.subscribe(dispatcherObserver)

//        whenCalled(responseBuilder.buildFilteredFillResponse(eq(activity), anyList())).thenReturn(filteredFillResponse)
//        whenCalled(responseBuilder.buildFallbackFillResponse(eq(activity))).thenReturn(fallbackFillResponse)

        PowerMockito.whenNew(FingerprintAuthDialogFragment::class.java).withAnyArguments()
            .thenReturn(fingerprintAuthDialogFragment)

        whenCalled(activity.supportFragmentManager).thenReturn(fragmentManager)
        whenCalled(navDestination.id).thenReturn(R.id.fragment_locked)
        whenCalled(navController.currentDestination).thenReturn(navDestination)
        PowerMockito.mockStatic(Navigation::class.java)
        whenCalled(Navigation.findNavController(activity, R.id.autofill_fragment_nav_host)).thenReturn(navController)

        subject = AutofillRoutePresenter(activity, responseBuilder, dispatcher, routeStore, dataStore, pslSupport)
        subject.onViewReady()
    }

    @Test
    fun `locked routes do nothing (default screen)`() {
//        routeStore.routeStub.onNext(RouteAction.LockScreen)
//
//        verifyZeroInteractions(activity)
//        verifyZeroInteractions(navController)
    }
}
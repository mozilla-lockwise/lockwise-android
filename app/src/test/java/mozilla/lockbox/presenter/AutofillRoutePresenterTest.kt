/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.autofill.IntentBuilder
import mozilla.lockbox.autofill.ParsedStructure
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AutofillStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.view.AutofillFilterFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
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
    Intent::class,
    RouteStore::class,
    DataStore::class
)
class AutofillRoutePresenterTest {

    @Mock
    val navController: NavController = mock(NavController::class.java)

    @Mock
    val navDestination: NavDestination = mock(NavDestination::class.java)

    @Mock
    val fingerprintAuthDialogFragment: FingerprintAuthDialogFragment =
        PowerMockito.mock(FingerprintAuthDialogFragment::class.java)

    @Mock
    val autofillFilterFragment: AutofillFilterFragment = PowerMockito.mock(AutofillFilterFragment::class.java)

    @Mock
    val fragmentManager: FragmentManager = mock(FragmentManager::class.java)

    @Mock
    val childFragmentManager: FragmentManager = mock(FragmentManager::class.java)

    @Mock
    val currentFragment: Fragment = mock(Fragment::class.java)

    @Mock
    val navHost: Fragment = mock(Fragment::class.java)

    @Mock
    val activity: AppCompatActivity = mock(AppCompatActivity::class.java)

    @Mock
    val intent: Intent = PowerMockito.mock(Intent::class.java)

    @Mock
    val keyguardManager = mock(KeyguardManager::class.java)!!

    @Mock
    val routeStore = PowerMockito.mock(RouteStore::class.java)!!

    @Mock
    val onBackPressedDispatcher: OnBackPressedDispatcher = mock(OnBackPressedDispatcher::class.java)

    @Mock
    val dataStore = PowerMockito.mock(DataStore::class.java)!!

    private val callingIntent = Intent()
    private val credentialIntent = Intent()

    class FakeResponseBuilder : FillResponseBuilder(ParsedStructure(packageName = "meow")) {
        @Mock
        val filteredFillResponse: FillResponse = mock(FillResponse::class.java)

        val asyncFilterStub = PublishSubject.create<List<ServerPassword>>()

        var filteredFillResponsePasswordsArgument: List<ServerPassword>? = null

        override fun buildFilteredFillResponse(
            context: Context,
            filteredPasswords: List<ServerPassword>
        ): FillResponse? {
            filteredFillResponsePasswordsArgument = filteredPasswords
            return filteredFillResponse
        }

        override fun asyncFilter(
            pslSupport: PublicSuffixSupport,
            list: Observable<List<ServerPassword>>
        ): Observable<List<ServerPassword>> {
            return asyncFilterStub
        }
    }

    private val routeStub = PublishSubject.create<RouteAction>()
    private val stateStub = PublishSubject.create<DataStore.State>()

    class FakeAutofillStore : AutofillStore() {
        val autofillActionStub = PublishSubject.create<AutofillAction>()
        override val autofillActions: Observable<AutofillAction>
            get() = autofillActionStub
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
    private val responseBuilder = FakeResponseBuilder()
    private val autofillStore = FakeAutofillStore()
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

        whenCalled(routeStore.routes).thenReturn(routeStub)
        whenCalled(dataStore.state).thenReturn(stateStub)
        whenCalled(dataStore.list).thenReturn(Observable.just(emptyList()))

        PowerMockito.whenNew(FingerprintAuthDialogFragment::class.java).withNoArguments()
            .thenReturn(fingerprintAuthDialogFragment)
        PowerMockito.whenNew(AutofillFilterFragment::class.java).withNoArguments()
            .thenReturn(autofillFilterFragment)
        PowerMockito.whenNew(Intent::class.java).withNoArguments()
            .thenReturn(intent)
        PowerMockito.whenNew(RouteStore::class.java).withAnyArguments()
            .thenReturn(routeStore)
        PowerMockito.whenNew(DataStore::class.java).withAnyArguments()
            .thenReturn(dataStore)

        whenCalled(activity.getString(ArgumentMatchers.anyInt())).thenReturn("hello")
        @Suppress("DEPRECATION")
        whenCalled(
            keyguardManager.createConfirmDeviceCredentialIntent(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
            )
        ).thenReturn(credentialIntent)
        whenCalled(activity.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager)
        whenCalled(childFragmentManager.fragments).thenReturn(listOf(currentFragment))
        whenCalled(navHost.childFragmentManager).thenReturn(childFragmentManager)
        whenCalled(fragmentManager.fragments).thenReturn(listOf(navHost))
        IntentBuilder.setSearchRequired(intent, true)
        whenCalled(activity.intent).thenReturn(callingIntent)
        whenCalled(activity.supportFragmentManager).thenReturn(fragmentManager)
        whenCalled(navDestination.id).thenReturn(R.id.fragment_null)
        whenCalled(navController.currentDestination).thenReturn(navDestination)
        PowerMockito.mockStatic(Navigation::class.java)
        whenCalled(Navigation.findNavController(activity, R.id.autofill_fragment_nav_host)).thenReturn(navController)
        whenCalled(activity.onBackPressedDispatcher).thenReturn(onBackPressedDispatcher)

        subject = AutofillRoutePresenter(
            activity,
            responseBuilder,
            dispatcher,
            routeStore,
            autofillStore,
            dataStore,
            pslSupport
        )
        subject.onViewReady()
    }

    @Test
    fun `locked routes`() {
        routeStub.onNext(RouteAction.LockScreen)
        verify(navController).navigate(R.id.fragment_locked, null, null)
    }

    @Test
    fun `item list routes navigate to filter backdrop`() {
        routeStub.onNext(RouteAction.ItemList)
        verify(navController).navigate(R.id.fragment_filter_backdrop, null, null)
    }

    @Test
    fun `autofill search dialog route route to autofill filter fragment`() {
        routeStub.onNext(RouteAction.DialogFragment.AutofillSearchDialog)

        verify(autofillFilterFragment).show(eq(fragmentManager), anyString())
        verify(autofillFilterFragment).setupDialog(R.string.autofill, null)
    }

    @Test
    fun `fingerprint dialog routes navigate to fingerprint dialog`() {
        val title = R.string.fingerprint_dialog_title
        routeStub.onNext(RouteAction.DialogFragment.FingerprintDialog(title))

        verify(fingerprintAuthDialogFragment).show(eq(fragmentManager), anyString())
        verify(fingerprintAuthDialogFragment).setupDialog(title, null)
    }

    @Test
    fun `cancel autofill actions`() {
        autofillStore.autofillActionStub.onNext(AutofillAction.Cancel)

        verify(activity).setResult(Activity.RESULT_CANCELED)
        verify(activity).finish()
    }

    @Test
    fun `complete single autofill actions`() {
        val login = ServerPassword(
            "fdsssddfs",
            "www.mozilla.org",
            "cats@cats.com",
            "dawgzone"
        )

        autofillStore.autofillActionStub.onNext(AutofillAction.Complete(login))

        Assert.assertEquals(listOf(login), responseBuilder.filteredFillResponsePasswordsArgument)

        verify(activity).setResult(eq(Activity.RESULT_OK), any<Intent>())
        verify(activity).finish()
        verify(intent).putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, responseBuilder.filteredFillResponse)
    }

    @Test
    fun `complete multiple autofill actions`() {
        val logins = listOf(
            ServerPassword(
                "fdsssddfs",
                "www.mozilla.org",
                "cats@cats.com",
                "dawgzone"
            ), ServerPassword(
                "fdsssddfs",
                "www.mozilla.org",
                "cats@cats.com",
                "dawgzone"
            )
        )

        autofillStore.autofillActionStub.onNext(AutofillAction.CompleteMultiple(logins))

        Assert.assertEquals(logins, responseBuilder.filteredFillResponsePasswordsArgument)

        verify(activity).setResult(eq(Activity.RESULT_OK), any<Intent>())
        verify(activity).finish()
        verify(intent).putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, responseBuilder.filteredFillResponse)
    }

    @Test
    fun `when the datastore is unlocked and the filtered list has logins`() {
        val logins = listOf(
            ServerPassword(
                "fdsssddfs",
                "www.mozilla.org",
                "cats@cats.com",
                "dawgzone"
            ), ServerPassword(
                "fdsssddfs",
                "www.mozilla.org",
                "cats@cats.com",
                "dawgzone"
            )
        )
        stateStub.onNext(DataStore.State.Unlocked)
        responseBuilder.asyncFilterStub.onNext(logins)

        val autofillCompleteAction = dispatcherObserver.values().last() as AutofillAction.CompleteMultiple
        assertEquals(logins, autofillCompleteAction.logins)
    }

    @Test
    fun `when the datastore is unlocked and the filtered list has no logins`() {
        stateStub.onNext(DataStore.State.Unlocked)
        responseBuilder.asyncFilterStub.onNext(emptyList())

        dispatcherObserver.assertLastValue(RouteAction.ItemList)
    }
}
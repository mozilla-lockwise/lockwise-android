/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.AppWebPageAction
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.autofill.IntentBuilder
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log
import mozilla.lockbox.model.DialogViewModel
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.AppWebPageFragmentArgs
import mozilla.lockbox.view.AutofillFilterFragment
import mozilla.lockbox.view.FingerprintAuthDialogFragment
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isA
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.isA
import org.mockito.Mockito.verify
import org.mockito.internal.matchers.Any
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled

fun <T> any(): T = Mockito.any<T>()

@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
    RoutePresenter::class,
    Navigation::class,
    Intent::class,
    AlertDialogHelper::class
)
class RoutePresenterTest {

    @Mock
    val activity: AppCompatActivity = Mockito.mock(AppCompatActivity::class.java)

    @Mock
    val context: Context = Mockito.mock(Context::class.java)

    @Mock
    val navController: NavController = Mockito.mock(NavController::class.java)

    @Mock
    val fragmentManager: FragmentManager = Mockito.mock(FragmentManager::class.java)

    @Mock
    val currentFragment: Fragment = Mockito.mock(Fragment::class.java)

    @Mock
    val navDestination: NavDestination = Mockito.mock(NavDestination::class.java)

    @Mock
    val fingerprintAuthDialogFragment: FingerprintAuthDialogFragment =
        PowerMockito.mock(FingerprintAuthDialogFragment::class.java)

    @Mock
    val dialogFragment = Mockito.mock(mozilla.lockbox.view.DialogFragment::class.java)

    @Mock
    val autofillFilterFragment: AutofillFilterFragment = PowerMockito.mock(AutofillFilterFragment::class.java)

    @Mock
    val intent: Intent = PowerMockito.mock(Intent::class.java)

    @Mock
    val settingIntent = Mockito.mock(SettingIntent.Security::class.java)

    @Mock
    val settingAction = Mockito.mock(RouteAction.SystemSetting::class.java)

    @Mock
    val action = Mockito.mock(Action::class.java)


    @Mock
    val dialogHelper = Mockito.mock(AlertDialogHelper::class.java)

    @Mock
    val dialogViewModel = Mockito.mock(DialogViewModel::class.java)


    @Mock
    val dialogBuilder = Mockito.mock(AlertDialog.Builder::class.java)

    @Mock
    val dialog = Mockito.mock(AlertDialog::class.java)

    @Mock
    val alertDialog = Mockito.mock(AlertDialog::class.java)

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
    val callingIntent = Intent()
    val alertState: Observable<AlertState>  = Observable.just(AlertState.BUTTON_NEGATIVE)

    lateinit var subject: RoutePresenter

    @Mock
    val alertDialogHelperStub = Mockito.mock(AlertDialogHelper::class.java)

    @Mock
    val windowMock = Mockito.mock(Window::class.java)


    class RoutePresenterStub(
        activity: AppCompatActivity,
        dispatcher: Dispatcher,
        routeStore: RouteStore,
        fragmentManagerStub: FragmentManager,
        currentFragmentStub: Fragment
    ) : RoutePresenter(activity, dispatcher, routeStore) {

        private val navHostFragmentManagerStub: FragmentManager = fragmentManagerStub
        override val navHostFragmentManager: FragmentManager
            get() = navHostFragmentManagerStub

        private val currentFragmentStub: Fragment = currentFragmentStub
        override val currentFragment: Fragment
            get() = currentFragmentStub
    }


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
        PowerMockito.mockStatic(AlertDialogHelper::class.java)

        PowerMockito.whenNew(Intent::class.java).withNoArguments()
            .thenReturn(intent)

        PowerMockito.whenNew(AlertDialogHelper::class.java).withNoArguments()
            .thenReturn(dialogHelper)

        IntentBuilder.setSearchRequired(intent, true)
        whenCalled(activity.intent).thenReturn(callingIntent)

        // testing `openSetting(...)`
        whenCalled(settingAction.setting).thenReturn(settingIntent)
        val securityIntent = "android.provider.Settings.ACTION_SECURITY_SETTINGS"
        whenCalled(settingIntent.intentAction).thenReturn(securityIntent)
        whenCalled(Intent(ArgumentMatchers.any(String::class.java))).thenReturn(callingIntent)

        // testing dialog methods
//        PowerMockito.whenNew(AlertDialogHelper::class.java).withAnyArguments()
//            .thenReturn(dialogHelper)
//        PowerMockito.whenNew(AlertDialogHelper::class.java).withAnyArguments()
//            .thenReturn(alertDialogHelperStub)
//        PowerMockito.whenNew(AlertDialogHelper::class.java).withNoArguments()
//            .thenReturn(alertDialogHelperStub)
//
//        PowerMockito.whenNew(AlertDialog.Builder::class.java).withAnyArguments()
//            .thenReturn(dialogBuilder)
//
//        PowerMockito.whenNew(AlertDialog::class.java).withAnyArguments()
//            .thenReturn(dialog)
//
//        PowerMockito.`when`(activity.applicationContext).thenReturn(context)
//        PowerMockito.`when`(activity.window).thenReturn(windowMock)
//        whenCalled(dialogBuilder.create()).thenReturn(alertDialog)
//        whenCalled(dialogBuilder.context).thenReturn(context)
//        whenCalled(activity.window).thenReturn(windowMock)


        // setup
        subject = RoutePresenterStub(
            activity,
            dispatcher,
            routeStore,
            fragmentManager,
            currentFragment
        )
        subject.navController = navController

//        activity.applicationContext.setTheme(R.style.AlertDialog_AppCompat)
    }

    @Test
    fun `security settings intent starts the activity`() {
        val settingAction = settingAction
        subject.openSetting(settingAction)
        verify(activity).startActivity(eq(ArgumentMatchers.any<Intent>()), null)
    }

    @Test
    fun `website url will create browser intent and start activity`() {
        val url = "com.meow"
        subject.openWebsite(url)
        verify(activity).startActivity(eq(ArgumentMatchers.any<Intent>()), null)
    }

    @Test
    fun `dialog fragment is set up`() {
        val destination = RouteAction.DialogFragment.FingerprintDialog(
            R.string.fingerprint_dialog_title,
            R.string.enable_fingerprint_dialog_subtitle
        )
        subject.showDialogFragment(dialogFragment, destination)
        verify(dialogFragment).setupDialog(destination.dialogTitle, destination.dialogSubtitle)
    }


    @Test
    fun `autofill dialog fragment is set up`() {
        val destination = RouteAction.DialogFragment.AutofillSearchDialog
        subject.showDialogFragment(dialogFragment, destination)
        verify(dialogFragment).setupDialog(destination.dialogTitle, null)
    }

    @Test
    fun `web view bundle is created`() {
        val action = AppWebPageAction.FaqList
        val result = subject.bundle(action)
        Assert.assertThat(result, instanceOf(Bundle::class.java))
    }

    @Test
    fun `item detail bundle is created`() {
        val action = RouteAction.ItemDetail("id")
        val result = subject.bundle(action)
        Assert.assertThat(result, instanceOf(Bundle::class.java))
    }

    @Test
    fun `find transition ids`() {
        val current = R.id.fragment_null
        val destination = R.id.fragment_locked

        val result = subject.findTransitionId(current, destination)
        Assert.assertEquals(R.id.action_to_locked, result)
    }
}
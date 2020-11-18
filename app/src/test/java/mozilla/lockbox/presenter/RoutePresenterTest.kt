/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.autofill.IntentBuilder
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.DialogFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
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
    val dialogFragment: DialogFragment = Mockito.mock(mozilla.lockbox.view.DialogFragment::class.java)

    @Mock
    val intent: Intent = PowerMockito.mock(Intent::class.java)

    @Mock
    val settingIntent: SettingIntent = Mockito.mock(SettingIntent.Security::class.java)

    @Mock
    val settingAction: RouteAction.SystemSetting = Mockito.mock(RouteAction.SystemSetting::class.java)

    @Mock
    val action: Action = Mockito.mock(Action::class.java)

    @Mock
    val dialogHelper: AlertDialogHelper = Mockito.mock(AlertDialogHelper::class.java)

    @Mock
    val keyguardManager: KeyguardManager = Mockito.mock(KeyguardManager::class.java)

    @Mock
    val routeStore: RouteStore = PowerMockito.mock(RouteStore::class.java)

    @Mock
    val onBackPressedDispatcher = Mockito.mock(OnBackPressedDispatcher::class.java)

    private val routeStub = PublishSubject.create<RouteAction>()

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
    private val callingIntent = Intent()
    private val credentialIntent = Intent()

    lateinit var subject: RoutePresenter

    class RoutePresenterStub(
        activity: AppCompatActivity,
        dispatcher: Dispatcher,
        routeStore: RouteStore,
        private val navHostFragmentManagerStub: FragmentManager,
        private val currentFragmentStub: Fragment
    ) : RoutePresenter(activity, dispatcher, routeStore) {

        override fun route(action: RouteAction) {}

        override fun findTransitionId(src: Int, dest: Int): Int? {
            return null
        }

        override val navHostFragmentManager: FragmentManager
            get() = navHostFragmentManagerStub

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
        whenCalled(activity.applicationContext).thenReturn(context)
        whenCalled(navDestination.id).thenReturn(R.id.fragment_null)
        whenCalled(navController.currentDestination).thenReturn(navDestination)
        whenCalled(routeStore.routes).thenReturn(routeStub)
        whenCalled(activity.onBackPressedDispatcher).thenReturn(onBackPressedDispatcher)
        PowerMockito.mockStatic(Navigation::class.java)
        PowerMockito.mockStatic(AlertDialogHelper::class.java)
        PowerMockito.whenNew(Intent::class.java).withNoArguments()
            .thenReturn(intent)
        PowerMockito.whenNew(AlertDialogHelper::class.java).withNoArguments()
            .thenReturn(dialogHelper)
        PowerMockito.whenNew(RouteStore::class.java).withNoArguments()
            .thenReturn(routeStore)

        whenCalled(activity.getString(anyInt())).thenReturn("hello")

        IntentBuilder.setSearchRequired(intent, true)
        whenCalled(activity.intent).thenReturn(callingIntent)
        whenCalled(settingAction.setting).thenReturn(settingIntent)
        @Suppress("DEPRECATION")
        whenCalled(
            keyguardManager.createConfirmDeviceCredentialIntent(
                anyString(),
                anyString()
            )
        ).thenReturn(credentialIntent)
        whenCalled(activity.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager)

        val securityIntent = "android.provider.Settings.ACTION_SECURITY_SETTINGS"
        whenCalled(settingIntent.intentAction).thenReturn(securityIntent)
        whenCalled(Intent(ArgumentMatchers.any(String::class.java))).thenReturn(callingIntent)

        subject = RoutePresenterStub(
            activity,
            dispatcher,
            routeStore,
            fragmentManager,
            currentFragment
        )
        subject.navController = navController
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
    fun `unlock fallback activity is started`() {
        val action = RouteAction.UnlockFallbackDialog

        subject.showUnlockFallback(action)

        verify(currentFragment).startActivityForResult(null, action.requestCode)
    }
}
package mozilla.lockbox

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AutoLockStore
import mozilla.lockbox.support.LockingSupport
import mozilla.lockbox.view.RootActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

class TestLockingSupport() : LockingSupport {
    override var systemTimeElapsed: Long = 0L
    override var currentBootId: String = UUID.randomUUID().toString()

    constructor(existing: LockingSupport) : this() {
        systemTimeElapsed = existing.systemTimeElapsed
        currentBootId = existing.currentBootId
    }

    fun advance(time: Long = 1L) {
        systemTimeElapsed += time
    }

    fun reboot() {
        currentBootId = UUID.randomUUID().toString()
    }
}

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
open class AutoLockTest {
    private val navigator = Navigator()
    private val testLockingSupport = TestLockingSupport(AutoLockStore.shared.lockingSupport)

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Before
    fun setUp() {
        AutoLockStore.shared.lockingSupport = testLockingSupport
    }

    @Test
    fun basicLockTest() {
        navigator.resetApp()
        simulateAppStartup()
        navigator.gotoItemList()
        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.FiveMinutes.ms + 1000)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtLockScreen()
    }

    @Test
    fun basicDontLockTest() {
        navigator.resetApp()
        simulateAppStartup()
        navigator.gotoItemList()
        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.OneMinute.ms)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtItemList()
    }

    @Test
    fun firstTimeLoginFlowInterruptTest() {
        navigator.resetApp()
        simulateAppStartup()
        navigator.gotoFxALogin()

        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.FiveMinutes.ms + 1000)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtFxALogin()
    }

    @Test
    fun disconnectAndReLoginFlowInterruptTest() {
        navigator.resetApp()
        simulateAppStartup()
        navigator.gotoItemList()

        Dispatcher.shared.dispatch(LifecycleAction.UserReset)
        Thread.sleep(200)

        navigator.checkAtWelcome()
        navigator.gotoFxALogin()

        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.FiveMinutes.ms + 1000)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtFxALogin()
    }

    private fun simulateAppStartup() {
        activityRule.activity.presenter.compositeDisposable.clear()
        activityRule.activity.presenter.onViewReady()
    }
}
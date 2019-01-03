package mozilla.lockbox

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AutoLockStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.LockingSupport
import mozilla.lockbox.view.RootActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class TestLockingSupport() : LockingSupport {
    override var systemTimeElapsed: Long = 0L

    constructor(existing: LockingSupport) : this() {
        systemTimeElapsed = existing.systemTimeElapsed
    }

    fun advance(time: Long = 1L) {
        systemTimeElapsed += time
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
        navigator.resetApp(activityRule)
    }

    @Test
    fun basicLockTest() {
        navigator.gotoItemList()
        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.FiveMinutes.ms + 1000)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtLockScreen()
    }

    @Test
    fun basicDontLockTest() {
        navigator.gotoItemList()
        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.OneMinute.ms)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtItemList()
    }

    @Test
    fun firstTimeLoginFlowInterruptTest() {
        navigator.resetApp(activityRule)
        navigator.gotoFxALogin()

        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.FiveMinutes.ms + 1000)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtFxALogin()
    }

    @Test
    fun disconnectAndReLoginFlowInterruptTest() {
        navigator.gotoItemList()

        Dispatcher.shared.dispatch(LifecycleAction.UserReset)
        DataStore.shared.state.blockingNext()

        navigator.checkAtWelcome()
        navigator.gotoFxALogin()

        Dispatcher.shared.dispatch(LifecycleAction.Background)

        testLockingSupport.advance(Setting.AutoLockTime.FiveMinutes.ms + 1000)

        Dispatcher.shared.dispatch(LifecycleAction.Foreground)

        navigator.checkAtFxALogin()
    }
}
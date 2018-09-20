package mozilla.lockbox

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import mozilla.lockbox.view.RootActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
open class RoutePresenterTest {
    private val navigator = Navigator()

    @Rule @JvmField
    val activityRule: ActivityTestRule<RootActivity> = ActivityTestRule(RootActivity::class.java)

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("mozilla.lockbox", appContext.packageName)
    }

    @Test
    fun testFxALogin() {
        navigator.gotoFxALogin()
    }

    @Test
    fun testItemList() {
        navigator.gotoItemList()
    }

    @Test
    fun testSettings() {
        navigator.gotoSettings()
    }
}

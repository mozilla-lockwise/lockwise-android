package mozilla.lockbox

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.runner.AndroidJUnit4
import android.support.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mozilla.lockbox.view.EntriesListFragment

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("mozilla.lockbox", appContext.packageName)
    }

    @Rule
    @JvmField
    val activity = ActivityTestRule<EntriesListFragment>(EntriesListFragment::class.java)

    @Test
    fun testHelloWorldText() {
        onView(withId(R.id.hello_world)).check(matches(withText("Hello World!")))
    }
}

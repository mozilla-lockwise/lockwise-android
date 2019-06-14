package mozilla.lockbox.uiTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.LockboxAutofillService
import mozilla.lockbox.flux.Dispatcher
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Ignore("589-UItests-update (#590)")
class LockboxAutofillServiceTest {

    private val dispatcher = Dispatcher()

    @get:Rule
    val serviceRule = ServiceTestRule()

    lateinit var subject: LockboxAutofillService

    @Before
    fun setUp() {
        subject = LockboxAutofillService(dispatcher = dispatcher)
        subject.onConnected()
    }

    @After
    fun tearDown() {
        subject.onDisconnected()
    }
}
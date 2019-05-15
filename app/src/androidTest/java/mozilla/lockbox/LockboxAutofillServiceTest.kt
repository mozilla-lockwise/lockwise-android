package mozilla.lockbox

import androidx.test.rule.ServiceTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Ignore("TODO")
class LockboxAutofillServiceTest {
    val twitterCredential = ServerPassword(
        "kjlfdsjlkf",
        "twitter.com",
        "cats@cats.com",
        "dawgzone"
    )

    private val dispatcher = Dispatcher()

    class FakeDataStore(dispatcher: Dispatcher) : DataStore(dispatcher = dispatcher) {
        override val list: Observable<List<ServerPassword>> = PublishSubject.create()
    }

    private val dataStore = FakeDataStore(dispatcher)

    @get:Rule
    val serviceRule = ServiceTestRule()

    lateinit var subject: LockboxAutofillService

    @Before
    fun setUp() {
        subject = LockboxAutofillService(dataStore = dataStore, dispatcher = dispatcher)
        subject.onConnected()
    }

    @After
    fun tearDown() {
        subject.onDisconnected()
    }
}
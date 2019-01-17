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
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class LockboxAutofillServiceTest {
    val twitterCredential = ServerPassword(
        "kjlfdsjlkf",
        "twitter.com",
        "cats@cats.com",
        "dawgzone"
    )

    class FakeDataStore : DataStore() {
        override val list: Observable<List<ServerPassword>> = PublishSubject.create()
    }

    private val dispatcher = Dispatcher()
    private val dataStore = FakeDataStore()

    @get:Rule
    val serviceRule = ServiceTestRule()

    val subject = LockboxAutofillService(dataStore, dispatcher)

    @Before
    fun setUp() {
        subject.onConnected()
    }

    @After
    fun tearDown() {
        subject.onDisconnected()
    }
}
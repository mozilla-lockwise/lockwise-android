package mozilla.lockbox

import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.SecurePreferences
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

    private val dispatcher = Dispatcher()

    class FakeDataStore(dispatcher: Dispatcher) : DataStore(dispatcher = dispatcher) {
        override val list: Observable<List<ServerPassword>> = PublishSubject.create()
    }

    class FakeAccountStore(dispatcher: Dispatcher, prefs: SecurePreferences) : AccountStore(dispatcher, prefs)

    private val prefs = SecurePreferences()
    private val dataStore = FakeDataStore(dispatcher)
    private val accountStore = FakeAccountStore(dispatcher, prefs)

    @get:Rule
    val serviceRule = ServiceTestRule()

    lateinit var subject: LockboxAutofillService

    @Before
    fun setUp() {
        prefs.injectContext(ApplicationProvider.getApplicationContext())
        subject = LockboxAutofillService(dataStore, accountStore, dispatcher)
        subject.onConnected()
    }

    @After
    fun tearDown() {
        subject.onDisconnected()
    }
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 *
 */

package mozilla.lockbox.support

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.DisposingTest
import mozilla.lockbox.extensions.assertLastValue
import mozilla.lockbox.extensions.assertLastValueMatches
import mozilla.lockbox.presenter.TestApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

typealias PublicSuffixPair = Pair<PublicSuffix, PublicSuffix>

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class PublicSuffixSupportTest : DisposingTest() {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val support = PublicSuffixSupport()

    @Before
    fun setUp() {
        support.injectContext(appContext)
    }

    @Test
    fun `test from web domain`() {
        var psObserver: TestObserver<PublicSuffix> = createTestObserver()
        support.fromWebDomain("example.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromWebDomain("www.example.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "www.example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromWebDomain("cats.example.co.uk")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.co.uk" }
            .assertLastValueMatches { it.fullDomain == "cats.example.co.uk" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromWebDomain("lockbox.mozilla")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "lockbox.mozilla" }
            .assertLastValueMatches { it.fullDomain == "lockbox.mozilla" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromWebDomain("blogspot.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "blogspot.com" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromWebDomain("")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromWebDomain(null)
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }
    }

    @Test
    fun `test from package name`() {
        var psObserver: TestObserver<PublicSuffix> = createTestObserver()
        support.fromPackageName("com.example")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromPackageName("com.example.android")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "android.example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromPackageName("uk.co.example.cats")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.co.uk" }
            .assertLastValueMatches { it.fullDomain == "cats.example.co.uk" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromPackageName("mozilla.lockbox")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "lockbox.mozilla" }
            .assertLastValueMatches { it.fullDomain == "lockbox.mozilla" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromPackageName("com.blogspot")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "blogspot.com" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromPackageName("")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromPackageName(null)
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }
    }

    @Test
    fun `test from hostname origin`() {
        var psObserver: TestObserver<PublicSuffix> = createTestObserver()
        support.fromOrigin("https://example.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromOrigin("http://example.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }
            .await()

        psObserver = createTestObserver()
        support.fromOrigin("https://www.example.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.com" }
            .assertLastValueMatches { it.fullDomain == "www.example.com" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromOrigin("https://cats.example.co.uk")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "example.co.uk" }
            .assertLastValueMatches { it.fullDomain == "cats.example.co.uk" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromOrigin("https://lockbox.mozilla")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "lockbox.mozilla" }
            .assertLastValueMatches { it.fullDomain == "lockbox.mozilla" }
            .assertLastValueMatches { !it.isEmpty() }
            .assertLastValueMatches { it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromOrigin("https://blogspot.com")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "blogspot.com" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromOrigin("")
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }

        psObserver = createTestObserver()
        support.fromOrigin(null)
            .subscribe(psObserver)
        psObserver.await()
            .assertLastValueMatches { it.topDomain == "" }
            .assertLastValueMatches { it.fullDomain == "" }
            .assertLastValueMatches { it.isEmpty() }
            .assertLastValueMatches { !it.isNotEmpty() }
    }

    @Test
    fun `test matching`() {
        var expected = support.fromWebDomain("www.example.com")
            .take(1)
        var candidate = support.fromWebDomain("www.example.com")
            .take(1)
        var matchObserver: TestObserver<PublicSuffixPair> = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { it.first.matches(it.second) }
            .assertLastValueMatches { it.second.matches(it.first) }

        candidate = support.fromWebDomain("mobile.example.com")
            .take(1)
        matchObserver = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { it.first.matches(it.second) }
            .assertLastValueMatches { it.second.matches(it.first) }

        candidate = support.fromPackageName("com.example.android")
            .take(1)
        matchObserver = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { it.first.matches(it.second) }
            .assertLastValueMatches { it.second.matches(it.first) }

        candidate = support.fromOrigin("https://login.example.com")
            .take(1)
        matchObserver = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { it.first.matches(it.second) }
            .assertLastValueMatches { it.second.matches(it.first) }

        candidate = support.fromWebDomain("areweloggedinyet.com")
            .take(1)
        matchObserver = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { !it.first.matches(it.second) }
            .assertLastValueMatches { !it.second.matches(it.first) }

        expected = support.fromWebDomain("cats.example.co.uk")
            .take(1)
        matchObserver = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { !it.first.matches(it.second) }
            .assertLastValueMatches { !it.second.matches(it.first) }

        candidate = support.fromWebDomain("cats.example.co.uk")
            .take(1)
        matchObserver = createTestObserver()
        Observables.combineLatest(expected, candidate)
            .subscribe(matchObserver)
        matchObserver.await()
            .assertLastValueMatches { it.first.matches(it.second) }
            .assertLastValueMatches { it.second.matches(it.first) }
    }

    @Test
    fun `filtering of ServerPasswords with PSL support`() {
        val domains = listOf("example.com", "firefox.com", "accounts.firefox.com", "mozilla.org")
        val passwords = domains.map { ServerPassword(id = it, hostname = "https://$it", username = it, password = it) }

        val (example, firefox1, firefox2) = passwords

        fun testFiltering(
            webDomain: String?,
            packageName: String = "com.android.chrome",
            vararg expected: ServerPassword
        ) {
            val matchObserver = createTestObserver<List<ServerPassword>>()
            Observable.just(passwords)
                .filter(support, webDomain, packageName)
                .subscribe(matchObserver)

            matchObserver.await()
                .assertLastValue(expected.toList())
        }

        testFiltering("firefox.com", "com.android.chrome", firefox1, firefox2)
        testFiltering(null, "com.example.android", example)
        testFiltering("api.facebook.com", "com.android.chrome") // empty list
    }
}
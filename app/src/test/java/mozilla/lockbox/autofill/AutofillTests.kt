/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.content.Context
import android.text.InputType
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.LockboxApplication
import mozilla.lockbox.autofill.AutofillNodeNavigator.Companion.editTextMask
import mozilla.lockbox.support.isDebug
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AutofillTests {

    val context: LockboxApplication = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        assumeTrue(isDebug())
    }

    @Test
    fun testBasicFixtures() {
        val files = listOf("test_basic", "test_labelled_edittexts", "test_basic_html_inputs", "test_labelled_html_inputs")

        files.map { DOMNavigator(context, it, it) }
            .forEach { navigator ->
                val subject = ParsedStructureBuilder(navigator).build()

                assertNotNull(subject.usernameId)
                assertTrue("${subject.usernameId!!} starts with u", subject.usernameId!!.startsWith("u"))
                assertNotNull(subject.passwordId)
                assertTrue("${subject.passwordId!!} starts with p", subject.passwordId!!.startsWith("p"))
            }
    }

    @Test
    fun testRealFixtures() {
        val fixtures = listOf(
            Fixture("app_twitter_2", null, "com.twitter.android"),
            Fixture("app_messenger_lite", null, "com.facebook.mlite"),
            Fixture("app_twitter", null, "com.twitter.android"),
            Fixture("html_twitter", "mobile.twitter.com", ""),
            Fixture("html_facebook", "m.facebook.com", ""),
            Fixture("html_gmail_1", "accounts.google.com", ""),
            Fixture("html_gmail_2", "accounts.google.com", ""),
            Fixture("app_fortuneo", null, "com.fortuneo.android"),

            // Fixtures using consecutive edit texts, the second with a password field.
            //            Fixture("app_facebook_lite", null, "com.facebook.lite"),
            // This currently works, but only in en_ locales.
            Fixture("app_facebook", null, "com.facebook.katana")
        )

        fixtures.forEach { fixture ->
            val navigator =
                DOMNavigator(context, fixture.filename, fixture.filename)
            val subject = ParsedStructureBuilder(navigator).build()

            assertNotNull("${fixture.filename} password detected", subject.passwordId)
            assertNotNull("${fixture.filename} username detected", subject.usernameId)

            if (fixture.webDomain != null) {
                assertEquals("${fixture.filename} webDomain detected", fixture.webDomain, subject.webDomain)
            } else {
                assertEquals("${fixture.filename} packageName detected", fixture.packageName, subject.packageName)
            }
        }
    }
}

data class Fixture(
    val filename: String,
    val webDomain: String?,
    val packageName: String?
)

class DOMNavigator(
    context: Context,
    filename: String,
    override val activityPackageName: String
) : AutofillNodeNavigator<Element, String> {

    override fun currentText(node: Element): String? {
        return node.getAttribute("autofillValue")
    }

    private val document: Document

    init {
        val inputStream = context.assets.open("fixtures/$filename.xml")
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        document = db.parse(inputStream)
    }

    override val rootNodes: List<Element>
        get() = listOf(document.documentElement)

    override fun childNodes(node: Element): List<Element> {
        val children = node.childNodes
        return (0 until children.length)
            .map { children.item(it) }
            .filter { it is Element }
            .map { it as Element }
    }

    override fun clues(node: Element): Iterable<CharSequence> {
        val attributes = node.attributes
        return (0 until attributes.length)
            .map { attributes.item(it) }
            .map { it.nodeValue }
    }

    override fun autofillId(node: Element): String? {
        return if (isEditText(node) || isHtmlInputField(node)) { clues(node).joinToString("|") } else { null }
    }

    override fun isEditText(node: Element): Boolean =
        node.tagName == "EditText" || (inputType(node) and editTextMask) > 0

    override fun isHtmlInputField(node: Element): Boolean {
        return node.tagName == "input"
    }

    override fun packageName(node: Element): String? {
        return node.attributes.getNamedItem("idPackage")?.nodeValue
    }

    override fun webDomain(node: Element): String? {
        return node.attributes.getNamedItem("webDomain")?.nodeValue
    }

    override fun inputType(node: Element): Int =
        node.attributes.getNamedItem("inputType")?.nodeValue?.let {
            Integer.parseInt(it, 16)
        } ?: 0

    override fun build(
        usernameId: String?,
        passwordId: String?,
        webDomain: String?,
        packageName: String
    ): ParsedStructureData<String> {
        return ParsedStructureData(usernameId, passwordId, webDomain, packageName)
    }
}

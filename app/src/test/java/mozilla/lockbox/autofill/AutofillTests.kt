/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.content.Context
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
                assertEquals("autofillId-username", subject.usernameId)
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

            // Fixtures with non-english hints.
            Fixture("app_fortuneo", null, "com.fortuneo.android"),

            // Fixtures using consecutive edit texts, the second with a password field.
            Fixture("app_facebook_lite", null, "com.facebook.lite"),
            Fixture("app_facebook", null, "com.facebook.katana"),

            // Fixtures with multiple input fields. Only consider fields that happen before a button.
            Fixture("html_auth0", "auth.mozilla.auth0.com", ""),

            // Fixtures with two web forms. Added a 'focus' attribute to the XML to select which is
            // focused.
            Fixture("html_amazon_signin", "www.amazon.co.uk", ""),
            Fixture("html_amazon_register", "www.amazon.co.uk", ""),
            Fixture("app_pocket_apple_id", "appleid.apple.com", "")
        )

        fixtures.forEach { fixture ->
            val navigator =
                DOMNavigator(context, fixture.filename, fixture.filename)
            val subject = ParsedStructureBuilder(navigator).build()

            assertNotNull("${fixture.filename} password detected", subject.passwordId)
            assertNotNull("${fixture.filename} username detected", subject.usernameId)
            assertEquals("autofillId-username", subject.usernameId)

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
            .mapNotNull { if (it.nodeName != "hint") it.nodeValue else null }
    }

    override fun autofillId(node: Element): String? {
        return if (isEditText(node) || isHtmlInputField(node)) {
            attr(node, "autofillId") ?: clues(node).joinToString("|")
        } else {
            null
        }
    }

    override fun isEditText(node: Element): Boolean =
        tagName(node) == "EditText" || (inputType(node) and editTextMask) > 0

    override fun isHtmlInputField(node: Element) = tagName(node) == "input"

    private fun tagName(node: Element) = node.tagName

    override fun isHtmlForm(node: Element): Boolean = node.tagName == "form"

    fun attr(node: Element, name: String) = node.attributes.getNamedItem(name)?.nodeValue

    override fun isFocused(node: Element) = attr(node, "focus") == "true"

    override fun isVisible(node: Element) = attr(node, "visibility")?.let { it == "0" } ?: true

    override fun packageName(node: Element) = attr(node, "idPackage")

    override fun webDomain(node: Element) = attr(node, "webDomain")

    override fun isButton(node: Element): Boolean {
        when (node.tagName) {
            "Button" -> return true
            "button" -> return true
        }

        return when (attr(node, "type")) {
            "submit" -> true
            "button" -> true
            else -> false
        }
    }

    override fun inputType(node: Element): Int =
        attr(node, "inputType")?.let {
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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId
import mozilla.lockbox.autofill.ParsedStructure
import mozilla.lockbox.autofill.ParsedStructureBuilder
import mozilla.lockbox.autofill.ServerPasswordBuilder
import mozilla.lockbox.autofill.ViewNodeNavigator
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled

class ServerPasswordBuilderTest {
    @Mock
    val assistStructure: AssistStructure = mock(AssistStructure::class.java)

    @Mock
    val windowNode: AssistStructure.WindowNode = mock(AssistStructure.WindowNode::class.java)

    @Mock
    val rootViewNode: AssistStructure.ViewNode = mock(AssistStructure.ViewNode::class.java)

    @Test
    fun `build server password from username and pw autofill fields`() {
        val usernameId = mock(AutofillId::class.java)
        val passwordId = mock(AutofillId::class.java)

        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)

        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
        whenCalled(usernameViewNode.className).thenReturn("android.widget.TextView")
        whenCalled(passwordViewNode.autofillId).thenReturn(passwordId)
        whenCalled(passwordViewNode.className).thenReturn("android.widget.TextView")

        whenCalled(rootViewNode.childCount).thenReturn(2)
        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameViewNode)
        whenCalled(rootViewNode.getChildAt(1)).thenReturn(passwordViewNode)

        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)

        val packageName = "package-name"
        val navigator = ViewNodeNavigator(assistStructure, packageName)
        val parsedStructure = ParsedStructureBuilder(navigator).build() as ParsedStructure


        val subject = ServerPasswordBuilder(parsedStructure, navigator).build()

        Assert.assertEquals(null, subject.username) // username is nullable, pw will always exist
        Assert.assertEquals("", subject.password)
        Assert.assertEquals(packageName, subject.hostname)
    }
}
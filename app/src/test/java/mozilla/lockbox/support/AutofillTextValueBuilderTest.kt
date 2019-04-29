/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import mozilla.lockbox.autofill.AutofillTextValueBuilder
import mozilla.lockbox.autofill.ParsedStructure
import mozilla.lockbox.autofill.ViewNodeNavigator
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled

class AutofillTextValueBuilderTest {
    @Mock
    val assistStructure: AssistStructure = mock(AssistStructure::class.java)

    @Mock
    val windowNode: AssistStructure.WindowNode = mock(AssistStructure.WindowNode::class.java)

    @Mock
    val rootViewNode: AssistStructure.ViewNode = mock(AssistStructure.ViewNode::class.java)

    @Test
    fun `build autofill text without values in username and pw autofill fields`() {
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

        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)

        val packageName = "package-name"
        val navigator = ViewNodeNavigator(assistStructure, packageName)
        val parsedStructure = ParsedStructure(usernameId, passwordId, packageName = packageName)

        val subject = AutofillTextValueBuilder(parsedStructure, navigator).build()

        Assert.assertEquals(null, subject.username)
        Assert.assertEquals(null, subject.password)
    }

    @Test
    fun `build autofill text from values in username and pw autofill fields`() {
        val usernameId = mock(AutofillId::class.java)
        val passwordId = mock(AutofillId::class.java)

        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
        val usernameViewValue = mock(AutofillValue::class.java)
        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)
        val passwordViewValue = mock(AutofillValue::class.java)

        val usernameValue = "example@example.com"
        val passwordValue = "iLUVkatz"

        whenCalled(usernameViewValue.isText).thenReturn(true)
        whenCalled(passwordViewValue.isText).thenReturn(true)
        whenCalled(usernameViewValue.textValue).thenReturn(usernameValue)
        whenCalled(passwordViewValue.textValue).thenReturn(passwordValue)

        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
        whenCalled(usernameViewNode.className).thenReturn("android.widget.TextView")
        whenCalled(usernameViewNode.autofillValue).thenReturn(usernameViewValue)
        whenCalled(passwordViewNode.autofillId).thenReturn(passwordId)
        whenCalled(passwordViewNode.className).thenReturn("android.widget.TextView")
        whenCalled(passwordViewNode.autofillValue).thenReturn(passwordViewValue)

        whenCalled(rootViewNode.childCount).thenReturn(2)
        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameViewNode)
        whenCalled(rootViewNode.getChildAt(1)).thenReturn(passwordViewNode)

        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)

        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)

        val packageName = "package-name"
        val navigator = ViewNodeNavigator(assistStructure, packageName)
        val parsedStructure = ParsedStructure(usernameId, passwordId, packageName = packageName)

        val subject = AutofillTextValueBuilder(parsedStructure, navigator).build()

        Assert.assertEquals(usernameValue, subject.username)
        Assert.assertEquals(passwordValue, subject.password)
    }
}
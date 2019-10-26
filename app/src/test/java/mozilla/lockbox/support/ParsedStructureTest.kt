package mozilla.lockbox.support

import android.app.assist.AssistStructure
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import mozilla.lockbox.autofill.ParsedStructureBuilder
import mozilla.lockbox.autofill.ViewNodeNavigator
import org.junit.Assert
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled

class ParsedStructureTest {
    @Mock
    val assistStructure: AssistStructure = mock(AssistStructure::class.java)

    @Mock
    val windowNode: AssistStructure.WindowNode = mock(AssistStructure.WindowNode::class.java)

    @Mock
    val rootViewNode = mock(AssistStructure.ViewNode::class.java)

    @Test
    fun usernameAndPasswordAutofillHintsOnEditText() {
        val usernameId = mock(AutofillId::class.java)
        val passwordId = mock(AutofillId::class.java)

        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(usernameViewNode.autofillHints).thenReturn(arrayOf(View.AUTOFILL_HINT_USERNAME))
        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
        whenCalled(usernameViewNode.className).thenReturn("android.widget.EditText")
        whenCalled(usernameViewNode.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)

        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(passwordViewNode.autofillHints).thenReturn(arrayOf(View.AUTOFILL_HINT_PASSWORD))
        whenCalled(passwordViewNode.autofillId).thenReturn(passwordId)
        whenCalled(passwordViewNode.className).thenReturn("android.widget.EditText")
        whenCalled(passwordViewNode.inputType).thenReturn(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        whenCalled(rootViewNode.childCount).thenReturn(2)
        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameViewNode)
        whenCalled(rootViewNode.getChildAt(1)).thenReturn(passwordViewNode)

        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)

        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)

        val navigator = ViewNodeNavigator(assistStructure, "caller-package-name")
        val subject = ParsedStructureBuilder(navigator).build()

        Assert.assertEquals(usernameId, subject.usernameId)
        Assert.assertEquals(passwordId, subject.passwordId)
    }

    @Test
    fun usernameAndPasswordAutofillHintsOnTextView() {
        val usernameId = mock(AutofillId::class.java)
        val passwordId = mock(AutofillId::class.java)

        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(usernameViewNode.autofillHints).thenReturn(arrayOf(View.AUTOFILL_HINT_USERNAME))
        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
        whenCalled(usernameViewNode.className).thenReturn("android.widget.TextView")
        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(passwordViewNode.autofillHints).thenReturn(arrayOf(View.AUTOFILL_HINT_PASSWORD))
        whenCalled(passwordViewNode.autofillId).thenReturn(passwordId)
        whenCalled(passwordViewNode.className).thenReturn("android.widget.TextView")

        whenCalled(rootViewNode.childCount).thenReturn(2)
        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameViewNode)
        whenCalled(rootViewNode.getChildAt(1)).thenReturn(passwordViewNode)

        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)

        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)

        val navigator = ViewNodeNavigator(assistStructure, "caller-package-name")
        val subject = ParsedStructureBuilder(navigator).build()

        Assert.assertNull(subject.usernameId)
        Assert.assertNull(subject.passwordId)
    }

    @Test
    fun onlyTextWithAutofillKeywordsOnEditText() {
        val usernameId = mock(AutofillId::class.java)
        val passwordId = mock(AutofillId::class.java)

        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
        whenCalled(usernameViewNode.text).thenReturn("Email Address")
        whenCalled(usernameViewNode.className).thenReturn("android.widget.EditText")
        whenCalled(usernameViewNode.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)
        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(passwordViewNode.autofillId).thenReturn(passwordId)
        whenCalled(passwordViewNode.text).thenReturn("Password")
        whenCalled(passwordViewNode.className).thenReturn("android.widget.EditText")
        whenCalled(passwordViewNode.inputType).thenReturn(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        whenCalled(rootViewNode.childCount).thenReturn(2)
        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameViewNode)
        whenCalled(rootViewNode.getChildAt(1)).thenReturn(passwordViewNode)

        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)

        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)

        val navigator = ViewNodeNavigator(assistStructure, "caller-package-name")
        val subject = ParsedStructureBuilder(navigator).build()

        Assert.assertEquals(usernameId, subject.usernameId)
        Assert.assertEquals(passwordId, subject.passwordId)
    }

    @Test
    fun textViewWithKeywordsFollowedByEditText() {
        val usernameTextViewId = mock(AutofillId::class.java)
        val usernameId = mock(AutofillId::class.java)
        val passwordTextViewId = mock(AutofillId::class.java)
        val passwordId = mock(AutofillId::class.java)

        val usernameTextViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(usernameTextViewNode.autofillId).thenReturn(usernameTextViewId)
        whenCalled(usernameTextViewNode.text).thenReturn("Email Address")
        whenCalled(usernameTextViewNode.className).thenReturn("android.widget.TextView")
        val usernameViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(usernameViewNode.autofillId).thenReturn(usernameId)
        whenCalled(usernameViewNode.className).thenReturn("android.widget.EditText")
        whenCalled(usernameViewNode.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)

        val passwordTextViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(passwordTextViewNode.autofillId).thenReturn(passwordTextViewId)
        whenCalled(passwordTextViewNode.text).thenReturn("Password")
        whenCalled(passwordTextViewNode.className).thenReturn("android.widget.TextView")
        val passwordViewNode = mock(AssistStructure.ViewNode::class.java)
        whenCalled(passwordViewNode.autofillId).thenReturn(passwordId)
        whenCalled(passwordViewNode.className).thenReturn("android.widget.EditText")
        whenCalled(passwordViewNode.inputType).thenReturn(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        whenCalled(rootViewNode.childCount).thenReturn(4)
        whenCalled(rootViewNode.getChildAt(0)).thenReturn(usernameTextViewNode)
        whenCalled(rootViewNode.getChildAt(1)).thenReturn(usernameViewNode)
        whenCalled(rootViewNode.getChildAt(2)).thenReturn(passwordTextViewNode)
        whenCalled(rootViewNode.getChildAt(3)).thenReturn(passwordViewNode)

        whenCalled(windowNode.rootViewNode).thenReturn(rootViewNode)

        whenCalled(assistStructure.windowNodeCount).thenReturn(1)
        whenCalled(assistStructure.getWindowNodeAt(0)).thenReturn(windowNode)

        val navigator = ViewNodeNavigator(assistStructure, "caller-package-name")
        val subject = ParsedStructureBuilder(navigator).build()

        Assert.assertEquals(usernameId, subject.usernameId)
        Assert.assertEquals(passwordId, subject.passwordId)
    }
}

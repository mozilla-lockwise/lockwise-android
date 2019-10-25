package mozilla.lockbox.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.text.InputType
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi
import mozilla.lockbox.autofill.AutofillNodeNavigator.Companion.editTextMask
import java.util.Locale

interface AutofillNodeNavigator<Node, Id> {
    companion object {
        val editTextMask = InputType.TYPE_CLASS_TEXT
        val passwordMask =
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    val rootNodes: List<Node>
    val activityPackageName: String
    fun childNodes(node: Node): List<Node>
    fun clues(node: Node): Iterable<CharSequence>
    fun autofillId(node: Node): Id?
    fun isEditText(node: Node): Boolean
    fun isHtmlInputField(node: Node): Boolean
    fun packageName(node: Node): String?
    fun webDomain(node: Node): String?
    fun currentText(node: Node): String?
    fun inputType(node: Node): Int
    fun isPasswordField(node: Node): Boolean = (inputType(node) and passwordMask) > 0
    fun build(
        usernameId: Id?,
        passwordId: Id?,
        webDomain: String?,
        packageName: String
    ): ParsedStructureData<Id>

    fun <T> findFirst(transform: (Node) -> T?): T? {
        rootNodes
            .forEach { node ->
                findFirst(node, transform)?.let { result ->
                    return result
                }
            }
        return null
    }

    fun <T> findFirst(node: Node, transform: (Node) -> T?): T? {
        transform(node)?.let {
            return it
        }

        childNodes(node)
            .forEach { child ->
                findFirst(child, transform)?.let { result ->
                    return result
                }
            }
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class ViewNodeNavigator(
    private val structure: AssistStructure,
    override val activityPackageName: String
) : AutofillNodeNavigator<ViewNode, AutofillId> {
    override val rootNodes: List<ViewNode>
        get() = structure.run { (0 until windowNodeCount).map { getWindowNodeAt(it).rootViewNode } }

    override fun childNodes(node: ViewNode): List<ViewNode> =
        node.run { (0 until childCount) }.map { node.getChildAt(it) }

    override fun clues(node: ViewNode): Iterable<CharSequence> {
        var hints = listOf(node.text, node.idEntry)

        node.autofillOptions?.let {
            hints += it
        }

        node.autofillHints?.let {
            hints += it
        }

        node.htmlInfo?.attributes?.let { attrs ->
            hints += attrs.map { it.second }
        }

        hints = hints.filterNotNull()
        return hints
    }

    override fun autofillId(node: ViewNode): AutofillId? = node.autofillId

    override fun isEditText(node: ViewNode) =
        inputType(node) and editTextMask > 0

    override fun inputType(node: ViewNode) = node.inputType

    override fun isHtmlInputField(node: ViewNode) =
        // Use English locale, as the HTML tags are all in English.
        node.htmlInfo?.tag?.toLowerCase(Locale.ENGLISH) == "input"

    override fun packageName(node: ViewNode): String? = node.idPackage

    override fun webDomain(node: ViewNode): String? = node.webDomain

    override fun currentText(node: ViewNode): String? {
        return if (node.autofillValue?.isText == true) {
            node.autofillValue?.textValue.toString()
        } else {
            null
        }
    }

    override fun build(
        usernameId: AutofillId?,
        passwordId: AutofillId?,
        webDomain: String?,
        packageName: String
    ): ParsedStructureData<AutofillId> {
        return ParsedStructure(
            usernameId,
            passwordId,
            webDomain,
            packageName
        )
    }
}

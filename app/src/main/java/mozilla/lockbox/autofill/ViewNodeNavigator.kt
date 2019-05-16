package mozilla.lockbox.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi

interface AutofillNodeNavigator<T, U> {
    val rootNodes: List<T>
    val activityPackageName: String
    fun childNodes(node: T): List<T>
    fun clues(node: T): Iterable<CharSequence>
    fun autofillId(node: T): U?
    fun isEditText(node: T): Boolean
    fun isHtmlInputField(node: T): Boolean
    fun packageName(node: T): String?
    fun webDomain(node: T): String?
    fun currentText(node: T): String?
    fun build(
        usernameId: U?,
        passwordId: U?,
        webDomain: String?,
        packageName: String
    ): ParsedStructureData<U>
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
        var hints = listOf(node.hint, node.text)

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

    override fun isEditText(node: ViewNode): Boolean {
        return node.className == "android.widget.EditText"
    }

    override fun isHtmlInputField(node: ViewNode): Boolean {
        return node.htmlInfo?.tag?.toLowerCase() == "input"
    }

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
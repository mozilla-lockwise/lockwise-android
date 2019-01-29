package mozilla.lockbox.support

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.os.Build
import android.view.View
import android.view.autofill.AutofillId
import mozilla.lockbox.extensions.childNodes

@TargetApi(Build.VERSION_CODES.O)
class ParsedStructureBuilder(
    private val structure: AssistStructure
) {
    fun build(): ParsedStructure {
        return ParsedStructure(
            getUsernameId(),
            getPasswordId(),
            getWebDomain(),
            getPackageId()
        )
    }

    private fun getUsernameId(): AutofillId? {
        // how do we localize the "email" and "username"?
        return getAutofillIdForKeywords(listOf(
            View.AUTOFILL_HINT_USERNAME,
            View.AUTOFILL_HINT_EMAIL_ADDRESS,
            "email",
            "username",
            "user name"
        ))
    }

    private fun getPasswordId(): AutofillId? {
        // similar l10n question for password
        return getAutofillIdForKeywords(listOf(View.AUTOFILL_HINT_PASSWORD, "password"))
    }

    private fun getAutofillIdForKeywords(keywords: Collection<String>): AutofillId? {
        return searchBasicAutofillContent(keywords) ?: checkForConsecutiveKeywordAndField(keywords)
    }

    private fun searchBasicAutofillContent(
        keywords: Collection<String>
    ): AutofillId? {
        return searchStructure(structure) { node ->
            if (isAutoFillableEditText(node, keywords) || isAutoFillableInputField(node, keywords)) {
                node.autofillId
            } else {
                null
            }
        }
    }

    private fun checkForConsecutiveKeywordAndField(keywords: Collection<String>): AutofillId? {
        return searchStructure(structure) { node ->
            val childNodes = node
                .run { (0 until childCount).map { getChildAt(it) } }

            // check for consecutive views with keywords followed by possible fill locations
            for (i in 1..(childNodes.size - 1)) {
                val prevNode = childNodes[i - 1]
                val currentNode = childNodes[i]
                val id = currentNode.autofillId ?: continue
                if (isEditText(currentNode) || isHtmlInputField(currentNode)) {
                    if (containsKeywords(prevNode, keywords)) {
                        return@searchStructure id
                    }
                }
            }
            null
        }
    }

    private fun getWebDomain(): String? {
        return searchStructure(structure) {
            it.webDomain
        }
    }

    private fun getPackageId(): String? {
        return searchStructure(structure) {
            it.idPackage
        }
    }

    private fun isAutoFillableEditText(
        node: AssistStructure.ViewNode,
        keywords: Collection<String>
    ): Boolean {
        return isEditText(node) &&
            containsKeywords(node, keywords) &&
            node.autofillId != null
    }

    private fun isEditText(node: AssistStructure.ViewNode) = node.className == "android.widget.EditText"

    private fun isAutoFillableInputField(
        node: AssistStructure.ViewNode,
        keywords: Collection<String>
    ): Boolean {
        return isHtmlInputField(node) &&
            containsKeywords(node, keywords) &&
            node.autofillId != null
    }

    private fun isHtmlInputField(node: AssistStructure.ViewNode) =
        node.htmlInfo?.tag?.toLowerCase() == "input"

    private fun <T> searchStructure(structure: AssistStructure, transform: (AssistStructure.ViewNode) -> T?): T? {
        structure
            .run { (0 until windowNodeCount).map { getWindowNodeAt(it).rootViewNode } }
            .forEach { node ->
                searchNodes(node, transform)?.let { result ->
                    return result
                }
            }
        return null
    }

    private fun <T> searchNodes(node: AssistStructure.ViewNode, transform: (AssistStructure.ViewNode) -> T?): T? {
        transform(node)?.let {
            return it
        }

        node.childNodes
            .forEach { child ->
                searchNodes(child, transform)?.let { result ->
                    return result
                }
            }
        return null
    }

    private fun containsKeywords(node: AssistStructure.ViewNode, keywords: Collection<String>): Boolean {
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

        keywords.forEach { keyword ->
            hints.forEach { hint ->
                if (hint.contains(keyword, true)) {
                    return true
                }
            }
        }
        return false
    }
}
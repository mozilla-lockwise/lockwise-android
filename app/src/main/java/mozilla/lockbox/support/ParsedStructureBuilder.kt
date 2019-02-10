package mozilla.lockbox.support

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.view.View
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi
import mozilla.lockbox.extensions.childNodes

@RequiresApi(Build.VERSION_CODES.O)
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

    private fun searchBasicAutofillContent(keywords: Collection<String>): AutofillId? {
        return findFirst(structure) { node ->
            if (isAutoFillableEditText(node, keywords) || isAutoFillableInputField(node, keywords)) {
                node.autofillId
            } else {
                null
            }
        }
    }

    private fun checkForConsecutiveKeywordAndField(keywords: Collection<String>): AutofillId? {
        return findFirst(structure) { node ->
            val childNodes = node
                .run { (0 until childCount).map { getChildAt(it) } }

            // check for consecutive views with keywords followed by possible fill locations
            for (i in 1..(childNodes.size - 1)) {
                val prevNode = childNodes[i - 1]
                val currentNode = childNodes[i]
                val id = currentNode.autofillId ?: continue
                if (isEditText(currentNode) || isHtmlInputField(currentNode)) {
                    if (containsKeywords(prevNode, keywords)) {
                        return@findFirst id
                    }
                }
            }
            null
        }
    }

    private fun getWebDomain(): String? {
        return nearestFocusedNode {
            it.webDomain
        }
    }

    private fun getPackageId(): String? {
        return nearestFocusedNode {
            it.idPackage
        }
    }

    private fun <T> nearestFocusedNode(transform: (ViewNode) -> T?): T? {
        val ancestors = findMatchedNodeAncestors(structure) {
            it.isFocused
        }
        return ancestors?.map(transform)?.first { it != null }
    }

    private fun isAutoFillableEditText(node: ViewNode, keywords: Collection<String>): Boolean {
        return isEditText(node) &&
            containsKeywords(node, keywords) &&
            node.autofillId != null
    }

    private fun isEditText(node: ViewNode) = node.className == "android.widget.EditText"

    private fun isAutoFillableInputField(node: ViewNode, keywords: Collection<String>): Boolean {
        return isHtmlInputField(node) &&
            containsKeywords(node, keywords) &&
            node.autofillId != null
    }

    private fun isHtmlInputField(node: ViewNode) =
        node.htmlInfo?.tag?.toLowerCase() == "input"

    private fun <T> findFirst(structure: AssistStructure, transform: (ViewNode) -> T?): T? {
        structure
            .run { (0 until windowNodeCount).map { getWindowNodeAt(it).rootViewNode } }
            .forEach { node ->
                findFirst(node, transform)?.let { result ->
                    return result
                }
            }
        return null
    }

    private fun <T> findFirst(node: ViewNode, transform: (ViewNode) -> T?): T? {
        transform(node)?.let {
            return it
        }

        node.childNodes
            .forEach { child ->
                findFirst(child, transform)?.let { result ->
                    return result
                }
            }
        return null
    }

    private fun containsKeywords(node: ViewNode, keywords: Collection<String>): Boolean {
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

    private fun findMatchedNodeAncestors(structure: AssistStructure, matcher: (ViewNode) -> Boolean):
        Iterable<ViewNode>? {
        structure
            .run { (0 until windowNodeCount).map { getWindowNodeAt(it).rootViewNode } }
            .forEach { node ->
                findMatchedNodeAncestors(node, matcher)?.let { result ->
                    return result
                }
            }
        return null
    }

    /**
     * Depth first search a ViewNode tree. Once a match is found, a list of ancestors all the way to the top is returned.
     * The first node in the list is the matching node, the last is the root node.
     * If no match is found, then <code>null</code> is returned.
     *
     * @param node the parent node.
     * @param matcher a closure which returns <code>true</code> if and only if the node is matched.
     * @return an ordered list of the matched node and all its ancestors starting at the matched node.
     */
    private fun findMatchedNodeAncestors(node: ViewNode, matcher: (ViewNode) -> Boolean):
        Iterable<ViewNode>? {

        if (matcher(node)) {
            return listOf(node)
        }

        node.childNodes
            .forEach { child ->
                findMatchedNodeAncestors(child, matcher)?.let { list ->
                    return list + node
                }
            }
        return null
    }
}
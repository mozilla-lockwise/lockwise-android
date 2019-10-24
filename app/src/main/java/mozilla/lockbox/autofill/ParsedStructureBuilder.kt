package mozilla.lockbox.autofill

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
class ParsedStructureBuilder<ViewNode, AutofillId>(
    private val navigator: AutofillNodeNavigator<ViewNode, AutofillId>
) {
    fun build(): ParsedStructureData<AutofillId> {
        val usernameId = getUsernameId()
        val passwordId = getPasswordId()
        val hostnameClue = usernameId ?: passwordId

        return navigator.build(
            usernameId,
            passwordId,
            getWebDomain(hostnameClue),
            getPackageName(hostnameClue) ?: navigator.activityPackageName
        )
    }

    private fun getUsernameId(): AutofillId? {
        // how do we localize the "email" and "username"?
        return getAutofillIdForKeywords(listOf(
            View.AUTOFILL_HINT_USERNAME,
            View.AUTOFILL_HINT_EMAIL_ADDRESS,
            "email",
            "username",
            "user name",
            "identifier"
        ))
    }

    private fun getPasswordId(): AutofillId? {
        // similar l10n question for password
        return getAutofillIdForKeywords(listOf(View.AUTOFILL_HINT_PASSWORD, "password"))
    }

    private fun getAutofillIdForKeywords(keywords: Collection<String>): AutofillId? {
        return searchBasicAutofillContent(keywords) ?: checkForConsecutiveKeywordAndField(keywords) ?: checkForNestedLayoutAndField(keywords)
    }

    private fun searchBasicAutofillContent(keywords: Collection<String>): AutofillId? {
        return navigator.findFirst { node: ViewNode ->
            if (isAutoFillableEditText(node, keywords) || isAutoFillableInputField(node, keywords)) {
                navigator.autofillId(node)
            } else {
                null
            }
        }
    }

    private fun checkForConsecutiveKeywordAndField(keywords: Collection<String>): AutofillId? {
        return navigator.findFirst { node: ViewNode ->
            val childNodes = navigator.childNodes(node)
            // check for consecutive views with keywords followed by possible fill locations
            for (i in 1.until(childNodes.size)) {
                val prevNode = childNodes[i - 1]
                val currentNode = childNodes[i]
                val id = navigator.autofillId(currentNode) ?: continue
                if (navigator.isEditText(currentNode) || navigator.isHtmlInputField(currentNode)) {
                    if (containsKeywords(prevNode, keywords)) {
                        return@findFirst id
                    }
                }
            }
            null
        }
    }

    private fun checkForNestedLayoutAndField(keywords: Collection<String>): AutofillId? {
        return navigator.findFirst { node: ViewNode ->
            val childNodes = navigator.childNodes(node)

            if (childNodes.size != 1) {
                return@findFirst null
            }

            val child = childNodes[0]
            val id = navigator.autofillId(child) ?: return@findFirst null
            if (navigator.isEditText(child) || navigator.isHtmlInputField(child)) {
                if (containsKeywords(node, keywords)) {
                    return@findFirst id
                }
            }
            null
        }
    }

    private fun getWebDomain(nearby: AutofillId?): String? {
        return nearestFocusedNode(nearby) {
            navigator.webDomain(it)
        }
    }

    private fun getPackageName(nearby: AutofillId?): String? {
        return nearestFocusedNode(nearby) {
            navigator.packageName(it)
        }
    }

    private fun <T> nearestFocusedNode(nearby: AutofillId?, transform: (ViewNode) -> T?): T? {
        val id = nearby ?: return null
        val ancestors = findMatchedNodeAncestors {
            navigator.autofillId(it) == id
        }
        return ancestors?.map(transform)?.firstOrNull { it != null }
    }

    private fun isAutoFillableEditText(node: ViewNode, keywords: Collection<String>): Boolean {
        return navigator.isEditText(node) &&
            containsKeywords(node, keywords) &&
            navigator.autofillId(node) != null
    }

    private fun isAutoFillableInputField(node: ViewNode, keywords: Collection<String>): Boolean {
        return navigator.isHtmlInputField(node) &&
            containsKeywords(node, keywords) &&
            navigator.autofillId(node) != null
    }

    private fun containsKeywords(node: ViewNode, keywords: Collection<String>): Boolean {
        val hints = navigator.clues(node)
        keywords.forEach { keyword ->
            hints.forEach { hint ->
                if (hint.contains(keyword, true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun findMatchedNodeAncestors(matcher: (ViewNode) -> Boolean): Iterable<ViewNode>? {
        navigator.rootNodes
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

        navigator.childNodes(node)
            .forEach { child ->
                findMatchedNodeAncestors(child, matcher)?.let { list ->
                    return list + node
                }
            }
        return null
    }
}

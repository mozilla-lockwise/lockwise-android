package mozilla.lockbox.support

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.os.Build
import android.view.View
import android.view.autofill.AutofillId
import mozilla.lockbox.log

@TargetApi(Build.VERSION_CODES.O)
class ParsedStructureBuilder(
    private val structure: AssistStructure
) {
    fun build(): ParsedStructure {
        val usernameId = getUsernameId(structure)
        val passwordId = getPasswordId(structure)
        val webDomain = null

        return ParsedStructure(usernameId, passwordId, webDomain)
    }

    private fun getUsernameId(structure: AssistStructure): AutofillId? {
        // how do we localize the "email" and "username"?
        return getAutofillIdForKeywords(structure, listOf(
            View.AUTOFILL_HINT_USERNAME,
            View.AUTOFILL_HINT_EMAIL_ADDRESS,
            "email",
            "username",
            "user name"
        ))
    }

    private fun getPasswordId(structure: AssistStructure): AutofillId? {
        // similar l10n question for password
        return getAutofillIdForKeywords(structure, listOf(View.AUTOFILL_HINT_PASSWORD, "password"))
    }

    private fun getAutofillIdForKeywords(structure: AssistStructure, keywords: Collection<String>): AutofillId? {
        val windowNodes = structure
            .run { (0 until windowNodeCount).map { getWindowNodeAt(it) } }

        var possibleAutofillIds = windowNodes
            .map { searchBasicAutofillContent(it.rootViewNode, keywords) }

        if (possibleAutofillIds.filterNotNull().isEmpty()) {
            possibleAutofillIds += windowNodes
                .map { checkForConsecutiveKeywordAndField(it.rootViewNode, keywords) }
        }

        return try {
            possibleAutofillIds.first { it != null }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun searchBasicAutofillContent(viewNode: AssistStructure.ViewNode?, keywords: Collection<String>): AutofillId? {
        val node = viewNode ?: return null

        if (isAutoFillableEditText(node, keywords) || isAutoFillableInputField(node, keywords)) {
            return node.autofillId
        }

        return try {
            node.run { (0 until childCount).map { getChildAt(it) } }
                .map { searchBasicAutofillContent(it, keywords) }
                .first { it != null }
        } catch (e: NoSuchElementException) {
            null
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
        log.info("node.htmlInfo?.tag? = ${node.htmlInfo?.tag ?: "null"}")
        return isHtmlInputField(node) &&
            containsKeywords(node, keywords) &&
            node.autofillId != null
    }

    private fun isHtmlInputField(node: AssistStructure.ViewNode) =
        node.htmlInfo?.tag?.toLowerCase() == "input"

    private fun checkForConsecutiveKeywordAndField(node: AssistStructure.ViewNode, keywords: Collection<String>): AutofillId? {
        val childNodes = node
            .run { (0 until childCount).map { getChildAt(it) } }

        // check for consecutive views with keywords followed by possible fill locations
        for (i in 1..(childNodes.size - 1)) {
            val prevNode = childNodes[i - 1]
            val currentNode = childNodes[i]
            val id = currentNode.autofillId ?: continue
            if (isEditText(currentNode) || isHtmlInputField(currentNode)) {
                if (containsKeywords(prevNode, keywords)) {
                    return id
                }
            }
        }

        return try {
            childNodes
                .map { checkForConsecutiveKeywordAndField(it, keywords) }
                .first { it != null }
        } catch (e: NoSuchElementException) {
            null
        }
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
package mozilla.lockbox.support

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.os.Build
import android.view.View
import android.view.autofill.AutofillId

@TargetApi(Build.VERSION_CODES.O)
class ParsedStructure(structure: AssistStructure) {
    var usernameId: AutofillId? = null
    var passwordId: AutofillId? = null

    init {
        usernameId = getUsernameId(structure)
        passwordId = getPasswordId(structure)
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

        if (containsKeywords(node, keywords) &&
            node.autofillId != null &&
            node.className == "android.widget.EditText") {
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

    private fun checkForConsecutiveKeywordAndField(node: AssistStructure.ViewNode, keywords: Collection<String>): AutofillId? {
        val childNodes = node
            .run { (0 until childCount).map { getChildAt(it) } }

        // check for consecutive views with keywords followed by possible fill locations
        for (i in 1..(childNodes.size - 1)) {
            val prevNode = childNodes[i - 1]
            val currentNode = childNodes[i]
            currentNode.autofillId?.let {
                if (containsKeywords(prevNode, keywords) &&
                    currentNode.className == "android.widget.EditText") {
                    return it
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
        val autofillHints = node.autofillHints?.toList() ?: emptyList()
        var hints = listOf(node.hint) + listOf(node.text) + autofillHints

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
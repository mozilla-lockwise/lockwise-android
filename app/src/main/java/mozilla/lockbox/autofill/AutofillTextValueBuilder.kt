/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.app.assist.AssistStructure.ViewNode
import android.view.autofill.AutofillId

data class AutofillTextValue(
    val username: String?,
    val password: String?
)

class AutofillTextValueBuilder(
    private val parsedStructure: ParsedStructure,
    private val navigator: AutofillNodeNavigator<ViewNode, AutofillId>
) {
    fun build(): AutofillTextValue {
        val usernameText = textForAutofillId(parsedStructure.username)
        val passwordText = textForAutofillId(parsedStructure.password)

        return AutofillTextValue(usernameText, passwordText)
    }

    private fun textForAutofillId(id: AutofillId?): String? {
        return findFirst { node ->
            if (navigator.autofillId(node) == id) {
                navigator.currentText(node)
            } else {
                null
            }
        }
    }

    private fun <T> findFirst(transform: (ViewNode) -> T?): T? {
        navigator.rootNodes
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

        navigator.childNodes(node)
            .forEach { child ->
                findFirst(child, transform)?.let { result ->
                    return result
                }
            }
        return null
    }
}
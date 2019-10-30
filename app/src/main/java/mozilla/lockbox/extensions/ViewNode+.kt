package mozilla.lockbox.extensions

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.os.Build

@TargetApi(Build.VERSION_CODES.O)
fun AssistStructure.ViewNode.dump(): String {
    val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    return dumpNode(sb).toString()
}

@TargetApi(Build.VERSION_CODES.O)
private fun AssistStructure.ViewNode.dumpNode(sb: StringBuilder = StringBuilder()): StringBuilder {

    val tag = htmlInfo?.tag?.let {
        if (it.isNotBlank()) {
            it
        } else {
            "UnspecifiedTag"
        }
    }
    val name = tag ?: className?.split('.')?.last() ?: "unknown"

    val viewAttrs = listOf(
        "idEntry" to idEntry,
        "idPackage" to idPackage,
        "idType" to idType,
        "webDomain" to webDomain,
        "hint" to hint,
        "autofillValue" to autofillValue?.let { if (it.isText) it.textValue.toString() else null },
        "autofillHints" to autofillHints?.joinToString(", "),
        "autofillOptions" to autofillOptions?.joinToString(", "),
        "inputType" to if (inputType > 0) inputType.toString(16) else null,
        "visibility" to visibility.toString(16) // visible == 0, invisible == 4, gone == 8
    )

    val htmlAttrs = htmlInfo?.attributes?.map { it.first to it.second } ?: emptyList()

    val attrs = (viewAttrs + htmlAttrs).filter { it.second != null }

    sb.append("<$name")
    if (!attrs.isEmpty()) {
        sb.append(" ")
        attrs.joinTo(sb, " ") { "${it.first}=\"${it.second}\"" }
    }

    if (childCount > 0) {
        sb.append(">")
        dumpChildren(sb)
        sb.append("</$name>")
    } else {
        sb.append("/>")
    }

    return sb
}

val AssistStructure.ViewNode.childNodes: List<AssistStructure.ViewNode>
    get() = (0 until childCount).map { getChildAt(it) }

@TargetApi(Build.VERSION_CODES.O)
private fun AssistStructure.ViewNode.dumpChildren(sb: StringBuilder) {
    childNodes
        .forEach {
            it.dumpNode(sb)
        }
}

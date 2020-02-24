/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import org.json.JSONObject

interface FromWebView
interface ToWebView

sealed class JS2KotlinMessage : FromWebView {
    data class TapBegin(val action: String) : JS2KotlinMessage()
    data class TapEnd(val action: String) : JS2KotlinMessage()
    data class Arrived(val destination: String) : JS2KotlinMessage()
    data class DestinationInformation(val destination: String, val options: FormInfo?) : JS2KotlinMessage()
    data class NodeInformation(val destination: String, val isDestination: Boolean, val numLinks: Int) : JS2KotlinMessage()
    data class FormFillSuccess(val formName: String) : JS2KotlinMessage()
    data class Fail(val action: String, val reason: AutoChangeError) : JS2KotlinMessage()
    data class HasInfo(val info: NodeInformation) : JS2KotlinMessage()
}

sealed class NavigationEvent : FromWebView {
    data class NavigatedTo(val url: String) : NavigationEvent()
    object GoneBack : NavigationEvent()
    data class ReloadedUrl(val url: String) : NavigationEvent()
}

sealed class NavigationMessage : ToWebView {
    //   - high level to start pop the next node off the agenda.
    //      If reset = true, then we set the url of the webview to the search node. This allows us to keep in sync.
    //      This does not go via JS.
    data class AgendaPop(val reset: Boolean = false) : NavigationMessage()
    //   - hit the browser back button
    //      This does not go via JS.
    object GoBack : NavigationMessage()

    //   - load the given url.
    //      This does not go via JS.
    data class LoadURL(val url: String) : NavigationMessage()

    // This is a terminator. It doesn't actually get sent to JS.
    object Done : NavigationMessage()

    data class Fail(val error: AutoChangeError) : NavigationMessage()
}

sealed class Kotlin2JSMessage(val action: String, vararg val args: String, val options: Map<String, String>? = null) : ToWebView {
    class Advance(destination: String) : Kotlin2JSMessage("advance", destination)
    class ExamineDestination(destination: String) : Kotlin2JSMessage("examine", destination)
    class FillForm(formName: String, inputValues: Map<String, String>) : Kotlin2JSMessage("fillForm", formName, options = inputValues)
    class ConfirmSuccess(formName: String, successIfPageChanged: Boolean) :
        Kotlin2JSMessage(
            "confirmSuccess",
            formName,
            if (successIfPageChanged) "true" else ""
        )

    // Five messages used in search.
    //   - checks page to see if a) we're at the destination, b) how many child nodes we have.
    //      The child nodes will be added to the agenda.
    class GetInfo(destination: String) : Kotlin2JSMessage("getNodeInfo", destination)
    //   - collects the relevant links and takes the index_th_ one.
    class NavigateTo(destination: String, index: Int) : Kotlin2JSMessage("navigateTo", destination, index.toString())
}

sealed class FormInfo {
    data class LoginFormInfo(val hostname: String?, val formActionOrigin: String?) : FormInfo()
    data class PasswordChangeInfo(val pattern: String?, val minLength: Int?, val maxLength: Int?) : FormInfo()

    companion object {
        fun loginInfo(obj: JSONObject): LoginFormInfo? {
            val formActionOrigin = obj.optString("formActionOrigin", null)
            val hostname = obj.optString("hostname", null)

            return if (hostname == null && formActionOrigin == null) {
                null
            } else {
                LoginFormInfo(hostname, formActionOrigin)
            }
        }

        fun passwordChangeInfo(obj: JSONObject): PasswordChangeInfo? {
            val pattern = obj.optString("pattern", null)
            val minLength = obj.optInt("minLength", 0)
            val maxLength = obj.optInt("maxLength", 255)

            return if (pattern == null && minLength == 0 && maxLength == 255) {
                null
            } else {
                PasswordChangeInfo(pattern, minLength, maxLength)
            }
        }
    }
}

interface JS2KotlinFFI {
    fun onTapBegin(token: String, action: String)
    fun onTapEnd(token: String, action: String)

    // These events are for "synchronous" messages,
    // that definitely will not span a page load.
    fun onLook(token: String, args: Array<String>)
    fun onFormFillSuccess(token: String, formName: String)
    fun onArrival(token: String, destination: String)
    fun onExamination(token: String, destination: String, opts: String)

    fun onFail(token: String, action: String, reason: String)
}

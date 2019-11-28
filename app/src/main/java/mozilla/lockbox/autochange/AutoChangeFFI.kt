/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import org.json.JSONObject

sealed class JS2KotlinMessage {
    data class TapBegin(val action: String) : JS2KotlinMessage()
    data class TapEnd(val action: String) : JS2KotlinMessage()
    data class Arrived(val destination: String) : JS2KotlinMessage()
    data class DestinationInformation(val destination: String, val options: FormInfo?) : JS2KotlinMessage()
    data class FormFillSuccess(val formName: String) : JS2KotlinMessage()
    data class Fail(val action: String, val reason: AutoChangeError) : JS2KotlinMessage()
}

sealed class Kotlin2JSMessage(val action: String, vararg val args: String, val options: Map<String, String>? = null) {
    class Advance(destination: String) : Kotlin2JSMessage("advance", destination)
    class ExamineDestination(destination: String) : Kotlin2JSMessage("examine", destination)
    class FillForm(formName: String, inputValues: Map<String, String>) : Kotlin2JSMessage("fillForm", formName, options = inputValues)
    class ConfirmSuccess(formName: String, successIfPageChanged: Boolean) :
        Kotlin2JSMessage(
            "confirmSuccess",
            formName,
            if (successIfPageChanged) "true" else ""
        )

    // This doesn't get sent to JS, but loads the URL directly
    class LoadURL(val url: String): Kotlin2JSMessage("loadURL")

    // This is a terminator. It doesn't actually get sent to JS.
    object Done : Kotlin2JSMessage("done")
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

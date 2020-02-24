/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import android.webkit.JavascriptInterface
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import mozilla.lockbox.log
import org.json.JSONObject

class WebViewJS2Kotlin(
    private val secureToken: String,
    val events: Subject<JS2KotlinMessage> = ReplaySubject.createWithSize<JS2KotlinMessage>(1)
) : JS2KotlinFFI {

    fun secure(token: String, andThen: () -> Unit) {
        if (token == secureToken) {
            andThen()
        }
    }

    @JavascriptInterface
    override fun onTapBegin(token: String, action: String) = secure(token) {
        events.onNext(JS2KotlinMessage.TapBegin(action))
    }

    @JavascriptInterface
    override fun onTapEnd(token: String, action: String) = secure(token) {
        events.onNext(JS2KotlinMessage.TapEnd(action))
    }

    @JavascriptInterface
    override fun onLook(token: String, args: Array<String>) = secure(token) {
        log.info("onLook: $args")
    }

    @JavascriptInterface
    override fun onFail(token: String, action: String, reason: String) = secure(token) {
        val error = AutoChangeError.valueOf(reason)
        events.onNext(JS2KotlinMessage.Fail(action, error))
    }

    @JavascriptInterface
    override fun onFormFillSuccess(token: String, formName: String) = secure(token) {
        events.onNext(JS2KotlinMessage.FormFillSuccess(formName))
    }

    @JavascriptInterface
    override fun onArrival(token: String, destination: String) = secure(token) {
        events.onNext(JS2KotlinMessage.Arrived(destination))
    }

    @JavascriptInterface
    override fun onExamination(token: String, destination: String, opts: String) = secure(token) {
        val options = JSONObject(opts)
        val formInfo = when (destination) {
            "login" -> FormInfo.loginInfo(options)
            "passwordChange" -> FormInfo.passwordChangeInfo(options)
            else -> null
        }
        events.onNext(JS2KotlinMessage.DestinationInformation(destination, formInfo))
    }
}

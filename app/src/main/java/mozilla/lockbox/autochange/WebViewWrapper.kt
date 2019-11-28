/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.annotation.RawRes
import com.github.satoshun.reactivex.webkit.data.OnPageFinished
import com.github.satoshun.reactivex.webkit.events
import io.reactivex.Observable
import mozilla.lockbox.BuildConfig
import mozilla.lockbox.R
import mozilla.lockbox.log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader

class WebViewWrapper(
    private val context: Context,
    private val webView: WebView,
    private val embeddedObject: WebViewJS2Kotlin,
    private val secureToken: String,
    private vararg val sustitutions: Pair<String, String>
) {
    val events
        get() = transformPageLoadToTapEnd(embeddedObject.events)

    fun setup() {
        setupWebViewDefaults(webView)
        setupJavascriptReturn(embeddedObject)
    }

    private val jsTemplate: String by lazy {
        val divider = "//".repeat(40)
        val jsFiles = arrayOf(
            R.raw.utils_ffi,
            R.raw.configuration,

            R.raw.utils_element,
            R.raw.utils_recognizers,
            R.raw.utils_form,
            R.raw.cmd_check_nuisances,

            R.raw.cmd_advance,
            R.raw.cmd_fill_form,
            R.raw.cmd_confirm_success,
            R.raw.cmd_examine_destination,

            R.raw.routing
        )
            .joinToString("\n$divider\n", transform = this::loadRawString)
            .replace(
                "\$secureToken" to secureToken,
                *sustitutions
            )

        loadRawString(R.raw.iife_template).replace("\$innerJavascript", jsFiles)
    }

    fun evalJSCommand(message: Kotlin2JSMessage) {
        val argList = message.options?.let {
            arrayOf(*message.args, JSONObject(it))
        } ?: message.args

        val argString = JSONArray(argList).toString()
        val jsString = jsTemplate.replace(
            "\$action" to message.action,
            "\$args" to argString
        )
        webView.evaluateJavascript(jsString) {
            log.debug("${message.action}$argString -> $it")
        }
    }

    fun loadUrl(url: String) {
        embeddedObject.events.onNext(JS2KotlinMessage.TapBegin("loading $url"))
        webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewDefaults(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = "mozilla.lockwise/${BuildConfig.VERSION_NAME}"
        webView.settings.domStorageEnabled = true
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // TODO we shouldn't be relying on clearing cookies; we should be able to detect that we've
        // arrived some place already.
        CookieManager.getInstance().removeAllCookies { /* NOOP */ }
    }

    private fun transformPageLoadToTapEnd(js2KotlinMessage: Observable<JS2KotlinMessage>): Observable<JS2KotlinMessage> {
        val pageFinished = webView.events()
            .ofType(OnPageFinished::class.java)

        // substitute the pageFinished events for TapEnd ones corresponding to the last TapBegin one.
        val simulatedTapEvents = pageFinished
            .flatMap {
                js2KotlinMessage
                    .ofType(JS2KotlinMessage.TapBegin::class.java)
                    .take(1)
            }
            .map { JS2KotlinMessage.TapEnd(it.action) }

        // merge the new simulated tap end events (that resulted in a page reload) with the
        // existing stream.
        return js2KotlinMessage
            .filter {
                it !is JS2KotlinMessage.TapBegin
            }
            .mergeWith(simulatedTapEvents)
    }

    private fun setupJavascriptReturn(lockwiseFFI: WebViewJS2Kotlin) =
        webView.addJavascriptInterface(lockwiseFFI, "lockwise_ffi")

    private fun String.replace(vararg substitutions: Pair<String, String>) =
        substitutions.fold(this) { js, (key, value) ->
            js.replace(key, value, false)
        }

    private fun loadRawString(@RawRes res: Int): String {
        val header = context.resources.getResourceEntryName(res)
        val inputStream = context.resources.openRawResource(res)
        return "// $header\n" + inputStream.bufferedReader().use(BufferedReader::readText)
    }

    fun finish() {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.settings.javaScriptEnabled = false
    }
}

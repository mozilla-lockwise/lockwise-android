/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.components.service.sync.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.autochange.WebViewAutoChangeHandler
import mozilla.lockbox.log

class UITestActivity : AppCompatActivity() {
    val compositeDisposable = CompositeDisposable()

    lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        statusText = findViewById(R.id.text_status)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    fun launchFingerprint(@Suppress("UNUSED_PARAMETER") view: View) {
        val dialogFragment = FingerprintAuthDialogFragment()
        val fragmentManager = this.supportFragmentManager
        try {
            dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(
                R.string.enable_fingerprint_dialog_title,
                R.string.enable_fingerprint_dialog_subtitle
            )
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }

    fun launchAutoChange(@Suppress("UNUSED_PARAMETER") view: View) {
//        val _500pxCredentials = ServerPassword(
//            id = "id",
//            hostname ="https://500px.com",
//            username = "lockwisedev@mailinator.com",
//            password = "qwertyui1"
//        )
//        val credentials = _500pxCredentials

        val disqusCredentials = ServerPassword(
            id = "id",
            hostname ="https://disqus.com",
            username = "lockwisedev@mailinator.com",
            password = "qwertyui1"
        )


        val dummyCredentials = ServerPassword(
            id = "id",
            hostname = "https://output.jsbin.com/gipodix",
            username = "lockwisedev@mailinator.com",
            password = "qwertyui1"
        )

        val credentials = if (false) {
            disqusCredentials
        } else {
            dummyCredentials
        }

//        val credentials = dummyCredentials

        val wv = findViewById<WebView>(R.id.internal_webview)

        val passwordChangeHandler = WebViewAutoChangeHandler(applicationContext, wv, credentials)

        passwordChangeHandler.progress
            .subscribe(
                {
                    val text = getText(it.message)
                    log.info("password changer: $text")
                    statusText.text = text
                },
                @SuppressLint("SetTextI18n") {
                    val text = it.message
                    log.error("password changer: $text")
                    statusText.text = "Error: $text"
                }
            )
            .addTo(compositeDisposable)

        passwordChangeHandler.invoke()
            .subscribe()
            .addTo(compositeDisposable)
    }
}

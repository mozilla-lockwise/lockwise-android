/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mozilla.lockbox.R
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_fxa_login.view.*
import kotlinx.android.synthetic.main.fragment_webview.*
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.presenter.NewWebView
import mozilla.lockbox.presenter.WebViewPresenter
import mozilla.lockbox.support.Constant

class WebViewFragment : BackableFragment(), NewWebView {
    override var webViewObserver: Consumer<String>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val url = arguments?.let { WebViewFragmentArgs.fromBundle(it).url }

        presenter = WebViewPresenter(this, url)

        var view = inflater.inflate(R.layout.fragment_webview, container, false)
        view.webView.settings.javaScriptEnabled = true

        if (url.equals(Constant.Faq.uri)) {
            view.toolbar.setTitle(R.string.nav_menu_faq)
        } else {
            view.toolbar.setTitle(R.string.nav_menu_feedback)
        }

        return view
    }

    override fun loadURL(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                webViewObserver?.accept(url)
                super.onPageStarted(view, url, favicon)
            }
        }
        webView.loadUrl(url)
    }
}

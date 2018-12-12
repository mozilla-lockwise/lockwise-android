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
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import kotlinx.android.synthetic.main.fragment_webview.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.WebViewPresenter
import mozilla.lockbox.presenter.NewWebView


class WebViewFragment : BackableFragment(), NewWebView {
    override var menuObserver: Consumer<String?>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        val itemSelection = view!!.navView.itemSelections().map { it.itemId }.
        presenter = WebViewPresenter(this)
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        view.navToolbar.setNavigationIcon(R.drawable.ic_close)
    }

    override fun loadUrl(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                menuObserver?.accept(url)
                super.onPageStarted(view, url, favicon)
            }
        }
        webView.loadUrl(url)
    }
}

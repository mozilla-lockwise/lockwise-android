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
import kotlinx.android.synthetic.main.fragment_faq.*
import kotlinx.android.synthetic.main.fragment_faq.view.webView
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.FaqPresenter
import mozilla.lockbox.presenter.FaqView
import mozilla.lockbox.support.Constant


class FaqFragment : BackableFragment(), FaqView {
    override var webViewObserver: Consumer<String?>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = FaqPresenter(this)
        return inflater.inflate(R.layout.fragment_faq, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.toolbar.setNavigationIcon(R.drawable.ic_close)
    }

    override fun loadUrl(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                webViewObserver?.accept(url)
                super.onPageStarted(view, url, favicon)
            }
        }
        webView.loadUrl(Constant.Faq.uri)
    }
}

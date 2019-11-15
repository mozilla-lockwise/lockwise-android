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
import androidx.annotation.StringRes
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_fxa_login.view.*
import kotlinx.android.synthetic.main.fragment_webview.*
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.AppWebPagePresenter
import mozilla.lockbox.presenter.WebPageView

class AppWebPageFragment : BackableFragment(), WebPageView {

    override var webViewObserver: Consumer<String>? = null
    private var url: String? = null
    private val errorHelper = NetworkErrorHelper()

    @StringRes
    private var toolbarTitle: Int? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let {
            url = AppWebPageFragmentArgs.fromBundle(it).url
            toolbarTitle = AppWebPageFragmentArgs.fromBundle(it).title
        }

        presenter = AppWebPagePresenter(this, url)

        val view = inflater.inflate(R.layout.fragment_webview, container, false)
        view.webView.settings.javaScriptEnabled = true
        view.toolbar.title = getString(toolbarTitle!!)

        return view
    }

    override fun onPause() {
        super.onPause()
        closeKeyboard(view)
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

    override fun handleNetworkError(networkErrorVisibility: Boolean) {
        if (!networkErrorVisibility) {
            errorHelper.showNetworkError(
                parent = view,
                child = view?.webView,
                topMarginId = R.dimen.network_error
            )
        } else {
            errorHelper.hideNetworkError(
                parent = view,
                child = view?.webView,
                topMarginId = R.dimen.hidden_network_error
            )
        }
    }

//    override val retryNetworkConnectionClicks: Observable<Unit>
//        get() = view!!.networkWarning.retryButton.clicks()
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_fxa_login.*
import kotlinx.android.synthetic.main.fragment_fxa_login.view.*
import kotlinx.android.synthetic.main.fragment_warning.view.*
import kotlinx.android.synthetic.main.include_backable.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.presenter.FxALoginPresenter
import mozilla.lockbox.presenter.FxALoginView
import mozilla.lockbox.support.isDebug

@ExperimentalCoroutinesApi
class FxALoginFragment : BackableFragment(), FxALoginView {
    private var errorHelper = NetworkErrorHelper()

    override var webViewRedirect: ((url: Uri?) -> Boolean) = { _ -> false }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = FxALoginPresenter(this)
        val view = inflater.inflate(R.layout.fragment_fxa_login, container, false)
        view.webView.settings.domStorageEnabled = true
        view.webView.settings.javaScriptEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isDebug()) {
            skipFxA.visibility = View.GONE
        }
        view.toolbar.setNavigationIcon(R.drawable.ic_close)
    }

    override fun loadURL(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                webViewRedirect(request?.url)
        }
        webView.loadUrl(url)
    }

    override fun handleNetworkError(networkErrorVisibility: Boolean) {
        if (!networkErrorVisibility) {
            errorHelper.showNetworkError(
                parent = view!!,
                child = view!!.webView,
                topMarginId = R.dimen.network_error_with_toolbar
            )
        } else {
            errorHelper.hideNetworkError(parent = view!!)
        }
    }

    override val retryNetworkConnectionClicks: Observable<Unit>
        get() = view!!.networkWarning.retryButton.clicks()

    override val skipFxAClicks: Observable<Unit>?
        get() = skipFxA?.clicks()

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }
}

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
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
import android.support.customtabs.CustomTabsIntent
import mozilla.lockbox.support.Constant
import java.util.Observable

class WebViewFragment : BackableFragment(), NewWebView {
    override var url: String? = null
    override var menuObserver: Observable<Int>
        get() {
//            val navView = view!!.navView
//            return navView.menu.map { it.itemId }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = WebViewPresenter(this)
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun loadUrl(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(this.activity, Uri.parse(url))
    }
}

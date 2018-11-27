/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.view.View
import android.webkit.WebView
import mozilla.lockbox.R
import mozilla.lockbox.flux.Presenter

class FaqPresenter(val view: View) : Presenter() {

    override fun onViewReady() {

        val faqView = view.findViewById<WebView>(R.id.faq_webview)
        faqView.loadUrl(R.string.faq_url.toString())
    }
}
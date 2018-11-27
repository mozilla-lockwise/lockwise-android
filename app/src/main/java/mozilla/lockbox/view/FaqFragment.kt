/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mozilla.lockbox.R
import mozilla.lockbox.presenter.FaqPresenter

class FaqFragment : CommonFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_faq, container, false)
        presenter = FaqPresenter(view)
//        val context = requireContext()
//        val view: WebView = WebView(context)
//        setContentView(R.layout.fragment_faq)
//        view.loadUrl("http://www.example.com")
        return view
    }

}
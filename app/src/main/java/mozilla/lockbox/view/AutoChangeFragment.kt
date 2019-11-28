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
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.fragment_autochange.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.log
import mozilla.lockbox.presenter.AutoChangePresenter
import mozilla.lockbox.presenter.AutoChangeView

@ExperimentalCoroutinesApi
class AutoChangeFragment : BackableFragment(), AutoChangeView {

    override val webView
        get() = view!!.webView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_autochange, container, false)

        val itemId = arguments?.let {
            AutoChangeFragmentArgs.fromBundle(it).itemId
        } ?: ""

        presenter = AutoChangePresenter(requireContext(), this, itemId)

        return view
    }

    override fun showProgressText(@StringRes message: Int) {
        val text = getString(message)
        log.info("Autochange: $text")
        view!!.progressToast.apply {
            progressText.text = text
            visibility = View.VISIBLE
        }
    }

    override fun hideProgressToast() {
        view?.progressToast?.visibility = View.GONE
    }
}

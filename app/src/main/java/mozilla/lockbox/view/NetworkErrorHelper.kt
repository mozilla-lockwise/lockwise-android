package mozilla.lockbox.view

import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_fxa_login.view.*
import kotlinx.android.synthetic.main.fragment_item_detail.view.*
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import kotlinx.android.synthetic.main.fragment_warning.view.*
import mozilla.lockbox.R
import mozilla.lockbox.log

open class NetworkErrorHelper {
    // START FxALoginFragment
    open fun showNetworkError(view: View) {
        log.error(view.resources.getString(R.string.networkWarningMessage))
        view.networkWarning.visibility = View.VISIBLE

        // set margin of webview to show warning message and toolbar
        val webParams = view.webView.layoutParams as ConstraintLayout.LayoutParams
        val toolbarPlusWarningMargin = view.resources.getInteger(R.integer.network_error_with_toolbar)
        webParams.setMargins(0, toolbarPlusWarningMargin, 0, 0)
        view.webView.layoutParams = webParams

        view.warningMessage.text = view.resources.getString(R.string.no_internet_connection)
    }

    open fun hideNetworkError(view: View) {
        log.info("Network successfully reconnected.")
        view.networkWarning.visibility = View.VISIBLE

        // set margin of webview to show only toolbar
        val webparams = view.webView.layoutParams as ConstraintLayout.LayoutParams
        val toolbarMargin = view.resources.getInteger(R.integer.actionBarSize)
        webparams.setMargins(0, toolbarMargin, 0, 0)
        view.webView.layoutParams = webparams
    }
    // END FxALoginFragment

    // START Item*Fragment
    open fun showItemNetworkError(view: View) {
        log.error(view.resources.getString(R.string.networkWarningMessage))
        view!!.networkWarning.visibility = View.VISIBLE
        view!!.warningMessage.text = view.resources.getString(R.string.no_internet_connection)
    }

    open fun hideItemListNetworkError(view: View) {
        log.info(view.resources.getString(R.string.network_connection_success))
        view.networkWarning.visibility = View.GONE
        view.networkWarning.layoutParams.height = R.dimen.hiddenNetworkError

        // set margin of entriesView to show only toolbar
        val entriesViewParams = view.entriesView.layoutParams as LinearLayout.LayoutParams
        entriesViewParams.setMargins(0, 0, 0, 0)
        view.entriesView.layoutParams = entriesViewParams
    }

    open fun hideItemDetailNetworkError(view: View) {
        log.info(view.resources.getString(R.string.network_connection_success))
        view.networkWarning.visibility = View.GONE
        view.networkWarning.layoutParams.height = R.dimen.hiddenNetworkError

        // set margin of card_view to show only toolbar
        val viewParams = view.card_view.layoutParams as ConstraintLayout.LayoutParams
        viewParams.setMargins(0, 0, 0, 0)
        view.card_view.layoutParams = viewParams
    }
    // END Item*Fragment

    // START AppWebPageFragment
    open fun showWebPageNetworkError(view: View) {
        log.error(view.resources.getString(R.string.networkWarningMessage))
        view.networkWarning.visibility = View.VISIBLE

        // set margin of webview to show warning message and toolbar
        val webParams = view.webView.layoutParams as ConstraintLayout.LayoutParams
        val toolbarPlusWarningMargin = view.resources.getInteger(R.integer.network_error)
        webParams.setMargins(0, toolbarPlusWarningMargin, 0, 0)
        view.webView.layoutParams = webParams

        view.warningMessage.text = view.resources.getString(R.string.no_internet_connection)
    }

    open fun hideWebPageNetworkError(view: View) {
        log.info("Network successfully reconnected.")
        view.networkWarning.visibility = View.VISIBLE

        // set margin of webview to show only toolbar
        val webparams = view.webView.layoutParams as ConstraintLayout.LayoutParams
        webparams.setMargins(0, 0, 0, 0)
        view.webView.layoutParams = webparams
    }
    // END AppWebPageFragment
}
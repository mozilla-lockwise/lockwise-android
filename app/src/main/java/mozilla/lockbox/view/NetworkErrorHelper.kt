package mozilla.lockbox.view

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.DimenRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_warning.view.*
import mozilla.lockbox.R
import mozilla.lockbox.log

open class NetworkErrorHelper {

    fun showNetworkError(parent: View?) {
        log.error(parent?.resources?.getString(R.string.networkWarningMessage))
        parent?.networkWarning?.visibility = View.VISIBLE
        parent?.warningMessage?.text = parent?.resources?.getString(R.string.no_internet_connection)
    }

    fun showNetworkError(parent: View?, child: WebView?, @DimenRes topMarginId: Int) {
        showNetworkError(parent)
        val webParams = child?.layoutParams as ConstraintLayout.LayoutParams
        val margin = parent?.resources?.getDimensionPixelSize(topMarginId)
        webParams.setMargins(0, margin ?: 0, 0, 0)
        child.layoutParams = webParams
    }

    fun hideNetworkError(parent: View?) {
        log.info(parent?.resources?.getString(R.string.network_connection_success))
        parent?.networkWarning?.visibility = View.GONE
        parent?.networkWarning?.layoutParams?.height = R.dimen.hidden_network_error
    }

    fun hideNetworkError(parent: View?, child: ViewGroup?, @DimenRes topMarginId: Int) {
        hideNetworkError(parent)
        val viewParams = child?.layoutParams as ConstraintLayout.LayoutParams
        val margin = parent?.resources?.getDimensionPixelSize(topMarginId)
        viewParams.setMargins(0, margin ?: 0, 0, 0)
        child.layoutParams = viewParams
    }

    fun hideNetworkError(parent: View?, child: RecyclerView?) {
        hideNetworkError(parent)
        val marginLayoutParams = ViewGroup.MarginLayoutParams(child?.layoutParams)
        marginLayoutParams.setMargins(0, 0, 0, 0)
        child?.layoutParams = marginLayoutParams
    }
}
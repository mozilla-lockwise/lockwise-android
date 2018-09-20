package mozilla.lockbox.view

import android.support.v4.app.Fragment
import android.os.Bundle
import kotlinx.android.synthetic.main.include_backable.view.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

open class BackableFragment : CommonFragment() {
    fun setupBackable(view: View) {
        view.toolbar.setNavigationIcon(android.R.drawable.arrow_up_float)

        view.toolbar.navigationClicks().subscribe { Dispatcher.shared.dispatch(RouteAction.BACK) }
    }
}
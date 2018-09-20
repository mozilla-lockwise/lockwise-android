package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

open class BackableFragment : CommonFragment() {
    fun setupBackable(view: View, backIcon: Int = android.R.drawable.arrow_up_float) {
        view.toolbar.setNavigationIcon(backIcon)
        view.toolbar.navigationClicks().subscribe { Dispatcher.shared.dispatch(RouteAction.BACK) }
    }
}
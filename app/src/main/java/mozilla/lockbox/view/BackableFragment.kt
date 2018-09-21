package mozilla.lockbox.view

import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.presenter.BackablePresenter
import mozilla.lockbox.presenter.BackableViewProtocol

open class BackableFragment : CommonFragment(), BackableViewProtocol {
    private lateinit var backablePresenter: BackablePresenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backablePresenter = BackablePresenter(this)
        backablePresenter.onViewReady()
    }

    fun setupBackable(view: View, backIcon: Int = android.R.drawable.arrow_up_float) {
        view.toolbar.setNavigationIcon(backIcon)
    }

    override val backButtonTaps: Observable<Unit>
        get() = view!!.toolbar.navigationClicks()
}

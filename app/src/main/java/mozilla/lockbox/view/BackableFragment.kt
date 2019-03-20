package mozilla.lockbox.view

import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.jakewharton.rxbinding2.support.v7.widget.navigationClicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.BackablePresenter
import mozilla.lockbox.presenter.BackableView

open class BackableFragment : Fragment(), BackableView {
    private lateinit var backablePresenter: BackablePresenter
    private lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navController = requireActivity().findNavController(R.id.fragment_nav_host)
        view.toolbar.setupWithNavController(navController)
        view.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        view.toolbar.setNavigationContentDescription(R.string.backable_description)

        backablePresenter = BackablePresenter(this)
        backablePresenter.onViewReady()

        super.onViewCreated(view, savedInstanceState)
    }

    override val backButtonClicks: Observable<Unit>
        get() = view!!.toolbar.navigationClicks().doOnEach {
            navController.popBackStack()
        }
}

package mozilla.lockbox.view

import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.include_backable.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.BackablePresenter
import mozilla.lockbox.presenter.BackableView

open class BackableFragment : Fragment(), BackableView {
    private lateinit var backablePresenter: BackablePresenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navController = requireActivity().findNavController(R.id.fragment_nav_host)
        view.toolbar.setupWithNavController(navController)
        view.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        view.toolbar.setNavigationContentDescription(R.string.backable_description)
        backablePresenter = BackablePresenter(this)
        backablePresenter.onViewReady()

        super.onViewCreated(view, savedInstanceState)
    }
}

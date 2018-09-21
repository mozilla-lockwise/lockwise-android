package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_fxa_login.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.FxALoginPresenter
import mozilla.lockbox.presenter.FxALoginViewProtocol

class FxALoginFragment : CommonFragment(), FxALoginViewProtocol {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = FxALoginPresenter(this)
        return inflater.inflate(R.layout.fragment_fxa_login, container, false)
    }

    override val logMeInClicks: Observable<Unit>
        get() = view!!.logMeInButton.clicks()
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.welcome_fragment.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.WelcomePresenter
import mozilla.lockbox.presenter.WelcomeViewProtocol

class WelcomeFragment : Fragment(), WelcomeViewProtocol {

    private lateinit var presenter: WelcomePresenter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        presenter = WelcomePresenter(this)
        return inflater.inflate(R.layout.welcome_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }

    override val getStartedClicks: Observable<Unit>
            get() = view!!.buttonGetStarted.clicks()
}
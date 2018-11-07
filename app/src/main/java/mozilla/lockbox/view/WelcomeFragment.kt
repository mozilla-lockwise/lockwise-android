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
import io.reactivex.Observable
import com.jakewharton.rxbinding2.view.clicks
import kotlinx.android.synthetic.main.fragment_welcome.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.WelcomePresenter
import mozilla.lockbox.presenter.WelcomeView

class WelcomeFragment : CommonFragment(), WelcomeView {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = WelcomePresenter(this)
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override val getStartedClicks: Observable<Unit>
            get() = view!!.buttonGetStarted.clicks()
}
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_locked.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.LockedPresenter
import mozilla.lockbox.presenter.LockedView

class LockedFragment : Fragment(), LockedView {
    private lateinit var presenter: LockedPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = LockedPresenter(this)
        return inflater.inflate(R.layout.fragment_locked, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onViewReady()
    }

    override val unlockButtonTaps: Observable<Unit>
        get() = view!!.unlockButton.clicks()

    override fun showFingerprintDialog() {
        val dialogFragment = FingerprintAuthDialogFragment()
        dialogFragment.show(fragmentManager, this.javaClass.simpleName)
    }
}
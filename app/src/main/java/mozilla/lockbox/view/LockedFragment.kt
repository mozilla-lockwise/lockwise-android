/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_locked.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.AppLockedPresenter
import mozilla.lockbox.presenter.LockedView

class LockedFragment : Fragment(), LockedView {
    private val _onActivityResult = PublishSubject.create<Pair<Int, Int>>()
    override val onActivityResult: Observable<Pair<Int, Int>> get() = _onActivityResult

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = AppLockedPresenter(this)
        return inflater.inflate(R.layout.fragment_locked, container, false)
    }

    override fun onPause() {
        super.onPause()
        closeKeyboard(view)
    }

    override val unlockButtonTaps: Observable<Unit>
        get() = requireView().unlockButton.clicks()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        _onActivityResult.onNext(Pair(requestCode, resultCode))
    }
}
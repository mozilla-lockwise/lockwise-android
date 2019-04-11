/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Intent
import android.os.Bundle
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.presenter.AutofillLockedPresenter
import mozilla.lockbox.presenter.LockedView

class AutofillLockedFragment : Fragment(), LockedView {
    override val unlockButtonTaps: Observable<Unit>? = null

    private val _onActivityResult = PublishSubject.create<Pair<Int, Int>>()
    override val onActivityResult: Observable<Pair<Int, Int>> get() = _onActivityResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = AutofillLockedPresenter(this)
        presenter.onViewReady()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        _onActivityResult.onNext(Pair(requestCode, resultCode))
    }
}
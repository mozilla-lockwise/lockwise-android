/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.R
import mozilla.lockbox.presenter.AutofillLockedPresenter
import mozilla.lockbox.presenter.LockedView
import mozilla.lockbox.support.Constant

class AutofillLockedFragment : Fragment(), LockedView {
    override val unlockButtonTaps: Observable<Unit>? = null

    private val _unlockConfirmed = PublishSubject.create<Boolean>()
    override val unlockConfirmed: Observable<Boolean> get() = _unlockConfirmed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = AutofillLockedPresenter(this)
        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun unlockFallback() {
        val manager = context?.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = manager.createConfirmDeviceCredentialIntent(
            getString(R.string.unlock_fallback_title),
            getString(R.string.confirm_pattern)
        )
        try {
            startActivityForResult(intent, Constant.RequestCode.lock)
        } catch (exception: Exception) {
            _unlockConfirmed.onNext(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constant.RequestCode.lock -> _unlockConfirmed.onNext(true)
            }
        } else {
            _unlockConfirmed.onNext(false)
        }
    }
}
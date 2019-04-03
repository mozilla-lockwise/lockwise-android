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
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_locked.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.AutofillLockedPresenter
import mozilla.lockbox.presenter.AutofillLockedView

private const val LOCK_REQUEST_CODE = 112

class AutofillLockedFragment : Fragment(), AutofillLockedView {
    private val _unlockConfirmed = PublishSubject.create<Boolean>()

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
            startActivityForResult(intent, LOCK_REQUEST_CODE)
        } catch (exception: Exception) {
            _unlockConfirmed.onNext(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                LOCK_REQUEST_CODE -> _unlockConfirmed.onNext(true)
            }
        } else {
            _unlockConfirmed.onNext(false)
        }
    }

    override val unlockConfirmed: Observable<Boolean> get() = _unlockConfirmed
}
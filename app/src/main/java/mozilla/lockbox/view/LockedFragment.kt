/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.content.Context
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
import mozilla.lockbox.presenter.LockedPresenter
import mozilla.lockbox.presenter.LockedView
import java.lang.Exception

class LockedFragment : CommonFragment(), LockedView {
    private val _unlockConfirmed = PublishSubject.create<Boolean>()

    companion object {
        private const val LOCK_REQUEST_CODE = 221
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = LockedPresenter(this)
        return inflater.inflate(R.layout.fragment_locked, container, false)
    }

    override val unlockButtonTaps: Observable<Unit>
        get() = view!!.unlockButton.clicks()

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
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                LOCK_REQUEST_CODE -> _unlockConfirmed.onNext(true)
            }
        } else {
            _unlockConfirmed.onNext(false)
        }
    }

    override val unlockConfirmed: Observable<Boolean> get() = _unlockConfirmed
}
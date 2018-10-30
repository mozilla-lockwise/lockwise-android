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
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.FingerprintDialogPresenter
import mozilla.lockbox.presenter.FingerprintDialogView

class FingerprintAuthDialogFragment : DialogFragment(), FingerprintDialogView {
    private val _authCallback = PublishSubject.create<AuthCallback>()
    override val authCallback: Observable<AuthCallback> get() = _authCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(android.support.v4.app.DialogFragment.STYLE_NORMAL, R.style.NoTitleDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = FingerprintDialogPresenter(this)
        return inflater.inflate(R.layout.fragment_fingerprint_dialog, container, false)
    }

    override fun onSucceeded() {
        view!!.fingerprintStatus.run {
            removeCallbacks(resetErrorTextRunnable)
            setTextColor(resources.getColor(R.color.green, null))
            text = getString(R.string.fingerprint_success)
        }
        view!!.imageView.run {
            setImageResource(R.drawable.ic_fingerprint_success)
            postDelayed({
                _authCallback.onNext(AuthCallback.OnAuth)
                dismiss()
            }, SUCCESS_DELAY_MILLIS)
        }
    }

    override fun onError(error: String?) {
        showError(error ?: getString(R.string.fingerprint_sensor_error))
        view!!.imageView.postDelayed({
            _authCallback.onNext(AuthCallback.OnError)
            dismiss()
        }, ERROR_TIMEOUT_MILLIS)
    }

    override fun onFailed(error: String?) {
        showError(error ?: getString(R.string.fingerprint_not_recognized))
    }

    override val cancelTapped: Observable<Unit>
        get() = view!!.cancel.clicks()

    override fun onCancel() {
        dismiss()
    }

    private fun showError(error: CharSequence) {
        view!!.imageView.setImageResource(R.drawable.ic_fingerprint_fail)
        view!!.fingerprintStatus.run {
            text = error
            setTextColor(resources.getColor(R.color.red, null))
            removeCallbacks(resetErrorTextRunnable)
            postDelayed(resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
        }
    }

    private val resetErrorTextRunnable = Runnable {
        view!!.imageView.setImageResource(R.drawable.ic_fingerprint)
        view!!.fingerprintStatus.run {
            setTextColor(resources.getColor(R.color.gray_73_percent, null))
            text = getString(R.string.touch_fingerprint_sensor)
        }
    }

    companion object {
        private const val ERROR_TIMEOUT_MILLIS: Long = 1600
        private const val SUCCESS_DELAY_MILLIS: Long = 1300
    }

    sealed class AuthCallback {
        object OnAuth : AuthCallback()
        object OnError : AuthCallback()
    }
}
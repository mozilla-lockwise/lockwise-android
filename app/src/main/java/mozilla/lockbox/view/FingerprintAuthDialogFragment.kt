/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.view.*
import mozilla.lockbox.R
import mozilla.lockbox.presenter.FingerprintDialogPresenter
import mozilla.lockbox.presenter.FingerprintDialogView

class FingerprintAuthDialogFragment : DialogFragment(), FingerprintDialogView {
    private lateinit var presenter: FingerprintDialogPresenter
    private val onAuth = PublishSubject.create<Unit>()
    private val onError = PublishSubject.create<Unit>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        presenter = FingerprintDialogPresenter(this)
        return inflater.inflate(R.layout.fragment_fingerprint_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onViewReady()
    }

    override fun onResume() {
        super.onResume()
        view!!.imageView.setImageResource(R.drawable.ic_fingerprint)
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
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
                onAuth::onNext
                dismiss()
            }, SUCCESS_DELAY_MILLIS)
        }
    }

    override fun onError(error: String?) {
        showError(error ?: getString(R.string.error))
        view!!.imageView.postDelayed({
            onError::onNext
            dismiss()
        }, ERROR_TIMEOUT_MILLIS)
    }

    override fun onFailed() {
        showError(getString(R.string.fingerprint_not_recognized))
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
            setTextColor(resources.getColor(R.color.darkGrey, null))
            text = getString(R.string.touch_fingerprint_sensor)
        }
    }

    companion object {
        private const val ERROR_TIMEOUT_MILLIS: Long = 1600
        private const val SUCCESS_DELAY_MILLIS: Long = 1300
    }

    override val onAuthenticated: Observable<Unit> get() = onAuth
    override val fallbackToPin: Observable<Unit> get() = onError
}
package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.view.*
import mozilla.lockbox.R

class OnboardingFingerprintAuthFragment : DialogFragment() {
    private val compositeDisposable = CompositeDisposable()
    private val _authCallback = PublishSubject.create<FingerprintAuthDialogFragment.AuthCallback>()
    val authCallback: Observable<FingerprintAuthDialogFragment.AuthCallback> get() = _authCallback
    private val _dismiss = PublishSubject.create<Unit>()
    val onDismiss: Observable<Unit> get() = _dismiss
    private var isEnablingDismissed: Boolean = true

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.onboarding_biometric_unlock, container, false)
    }

//overrides view
    fun onSucceeded() {
        view!!.fingerprintStatus.run {
            removeCallbacks(resetErrorTextRunnable)
            setTextColor(resources.getColor(R.color.green, null))
            text = getString(R.string.fingerprint_success)
        }
        view!!.imageView.run {
            setImageResource(R.drawable.ic_fingerprint_success)
            postDelayed({
                _authCallback.onNext(FingerprintAuthDialogFragment.AuthCallback.OnAuth)
                isEnablingDismissed = false
                dismiss()
            }, SUCCESS_DELAY_MILLIS)
        }
    }

//overrides view
    fun onError(error: String?) {
        showError(error ?: getString(R.string.fingerprint_sensor_error))
        view!!.imageView.postDelayed({
            _authCallback.onNext(FingerprintAuthDialogFragment.AuthCallback.OnError)
            dismiss()
        }, ERROR_TIMEOUT_MILLIS)
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
}
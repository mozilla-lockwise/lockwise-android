package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import mozilla.lockbox.R
import mozilla.lockbox.presenter.OnboardingFingerprintAuthPresenter

class OnboardingFingerprintAuthFragment : Fragment() {
    private val compositeDisposable = CompositeDisposable()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        presenter = OnboardingFingerprintAuthPresenter(this)

        return inflater.inflate(R.layout.onboarding_biometric_unlock, container, false)
    }




















//    private val _authCallback = PublishSubject.create<FingerprintAuthDialogFragment.AuthCallback>()
//    val authCallback: Observable<FingerprintAuthDialogFragment.AuthCallback> get() = _authCallback
//    private val _dismiss = PublishSubject.create<Unit>()
//    val onDismiss: Observable<Unit> get() = _dismiss
//    private var isEnablingDismissed: Boolean = true


//    private val resetErrorTextRunnable = Runnable {
//        view!!.imageView.setImageResource(R.drawable.ic_fingerprint)
//        view!!.fingerprintStatus.run {
//            setTextColor(resources.getColor(R.color.gray_73_percent, null))
//            text = getString(R.string.touch_fingerprint_sensor)
//        }
//    }


//    companion object {
//        private const val ERROR_TIMEOUT_MILLIS: Long = 1600
//        private const val SUCCESS_DELAY_MILLIS: Long = 1300
//    }
//
//    sealed class AuthCallback {
//        object OnAuth : AuthCallback()
//        object OnError : AuthCallback()
//    }


//
////overrides view
//    fun onSucceeded() {
//        view!!.fingerprintStatus.run {
//            removeCallbacks(resetErrorTextRunnable)
//            setTextColor(resources.getColor(R.color.green, null))
//            text = getString(R.string.fingerprint_success)
//        }
//        view!!.imageView.run {
//            setImageResource(R.drawable.ic_fingerprint_success)
//            postDelayed({
//                _authCallback.onNext(FingerprintAuthDialogFragment.AuthCallback.OnAuth)
//                isEnablingDismissed = false
//                close()
//            }, SUCCESS_DELAY_MILLIS)
//        }
//    }
//
////overrides view
//    fun onError(error: String?) {
//        showError(error ?: getString(R.string.fingerprint_sensor_error))
//        view!!.imageView.postDelayed({
//            _authCallback.onNext(FingerprintAuthDialogFragment.AuthCallback.OnError)
//            close()
//        }, ERROR_TIMEOUT_MILLIS)
//    }
//
//    private fun showError(error: CharSequence) {
//        view!!.imageView.setImageResource(R.drawable.ic_fingerprint_fail)
//        view!!.fingerprintStatus.run {
//            text = error
//            setTextColor(resources.getColor(R.color.red, null))
//            removeCallbacks(resetErrorTextRunnable)
//            postDelayed(resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
//        }
//    }
//
//    fun close() {
//        //close onboarding fragment
//    }
}
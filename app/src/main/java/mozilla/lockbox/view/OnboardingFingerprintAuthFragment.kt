package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.onboarding_biometric_unlock.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.presenter.OnboardingFingerprintAuthPresenter
import mozilla.lockbox.presenter.OnboardingFingerprintView
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.support.Constant

@ExperimentalCoroutinesApi
class OnboardingFingerprintAuthFragment : Fragment(), OnboardingFingerprintView {
    private val _authCallback = PublishSubject.create<FingerprintAuthCallback>()
    override val authCallback: Observable<FingerprintAuthCallback> get() = _authCallback

    override val onDismiss: Observable<Unit>
        get() = view!!.skipOnboarding.clicks()

    private var isEnablingDismissed: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = OnboardingFingerprintAuthPresenter(this)
        return inflater.inflate(R.layout.onboarding_biometric_unlock, container, false)
    }

    override fun onSucceeded() {
        view!!.sensorDescription.setTextColor(resources.getColor(R.color.green, null))
        view!!.sensorDescription.text = getString(R.string.fingerprint_success)
        view!!.iconFingerprint.setImageResource(R.drawable.ic_fingerprint_success)

        view!!.sensorDescription.removeCallbacks(resetErrorTextRunnable)
        view!!.iconFingerprint.postDelayed({
            _authCallback.onNext(FingerprintAuthCallback.OnAuth)
            isEnablingDismissed = false
        }, Constant.FingerprintTimeout.successDelayMillis)
    }

    override fun onFailed(error: String?) {
        showError(error ?: getString(R.string.fingerprint_not_recognized))
    }

    override fun onError(error: String?) {
        showError(error ?: getString(R.string.fingerprint_sensor_error))
        view!!.postDelayed(
            { _authCallback.onNext(FingerprintAuthCallback.OnError) },
            Constant.FingerprintTimeout.errorTimeoutMillis
        )
    }

    private fun showError(error: CharSequence) {
        view!!.iconFingerprint.setImageResource(R.drawable.ic_fingerprint_fail)
        view!!.sensorDescription.run {
            text = error
            setTextColor(resources.getColor(R.color.red, null))
            removeCallbacks(resetErrorTextRunnable)
            postDelayed(resetErrorTextRunnable, Constant.FingerprintTimeout.errorTimeoutMillis)
        }
    }

    private val resetErrorTextRunnable = Runnable {
        view!!.iconFingerprint.setImageResource(R.drawable.ic_fingerprint)
        view!!.sensorDescription.run {
            setTextColor(resources.getColor(R.color.gray_73_percent, null))
            text = getString(R.string.touch_fingerprint_sensor)
        }
    }
}
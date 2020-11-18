package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_onboarding.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.presenter.FingerprintOnboardingPresenter
import mozilla.lockbox.presenter.FingerprintOnboardingView
import mozilla.lockbox.support.Constant

@ExperimentalCoroutinesApi
class FingerprintOnboardingFragment : Fragment(), FingerprintOnboardingView {
    private val _authCallback = PublishSubject.create<FingerprintAuthAction>()
    override val authCallback: Observable<FingerprintAuthAction> get() = _authCallback

    override val onSkipClick: Observable<Unit>
        get() = requireView().skipButton.clicks()

    private var isEnablingDismissed: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        presenter = FingerprintOnboardingPresenter(this)
        val view = inflater.inflate(R.layout.fragment_fingerprint_onboarding, container, false)
        val appLabel = getString(R.string.app_label)
        view.unlockWithFingerprintTitle.text = getString(R.string.onboarding_unlock_title, appLabel)
        view.unlockDescription.text = getString(R.string.onboarding_unlock_description, appLabel)
        return view
    }

    override fun onSucceeded() {
        requireView().apply {
            sensorDescription.setTextColor(resources.getColor(R.color.green, null))
            sensorDescription.text = getString(R.string.fingerprint_success)
            iconFingerprint.setImageResource(R.drawable.ic_fingerprint_success)

            sensorDescription.removeCallbacks(resetErrorTextRunnable)
            iconFingerprint.postDelayed({
                _authCallback.onNext(FingerprintAuthAction.OnSuccess)
                isEnablingDismissed = false
            }, Constant.FingerprintTimeout.successDelayMillis)
        }
    }

    override fun onFailed(error: String?) {
        showError(error ?: getString(R.string.fingerprint_not_recognized))
    }

    override fun onError(error: String?) {
        showError(error ?: getString(R.string.fingerprint_sensor_error))
        requireView().postDelayed(
            { _authCallback.onNext(FingerprintAuthAction.OnError) },
            Constant.FingerprintTimeout.errorTimeoutMillis
        )
    }

    private fun showError(error: CharSequence) {
        requireView().iconFingerprint.setImageResource(R.drawable.ic_fingerprint_fail)
        requireView().sensorDescription.run {
            text = error
            setTextColor(resources.getColor(R.color.red, null))
            removeCallbacks(resetErrorTextRunnable)
            postDelayed(resetErrorTextRunnable, Constant.FingerprintTimeout.errorTimeoutMillis)
        }
    }

    private val resetErrorTextRunnable = Runnable {
        requireView().iconFingerprint.setImageResource(R.drawable.ic_fingerprint)
        requireView().sensorDescription.run {
            setTextColor(resources.getColor(R.color.gray_73_percent, null))
            text = getString(R.string.touch_fingerprint_sensor)
        }
    }
}
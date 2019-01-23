package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.view.*
import kotlinx.android.synthetic.main.onboarding_biometric_unlock.view.*
import mozilla.lockbox.R
import mozilla.lockbox.log
import mozilla.lockbox.presenter.OnboardingFingerprintAuthPresenter
import mozilla.lockbox.presenter.OnboardingFingerprintView
import mozilla.lockbox.store.RouteStore

class OnboardingFingerprintAuthFragment : Fragment(), OnboardingFingerprintView {

    private val compositeDisposable = CompositeDisposable()

    private val _dismiss = PublishSubject.create<Unit>()
    override val onDismiss: Observable<Unit> = _dismiss

    private val _authCallback = PublishSubject.create<AuthCallback>()
    override val authCallback: Observable<AuthCallback> get() = _authCallback

    private var isEnablingDismissed: Boolean = true

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

        presenter = OnboardingFingerprintAuthPresenter(this)

        return inflater.inflate(R.layout.onboarding_biometric_unlock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }

    override fun onDestroyView() {
        log.info("Onboarding fingerprint biometric enablement dismissed.")
        if (isEnablingDismissed) {
            _dismiss.onNext(Unit)
        }
        compositeDisposable.clear()

        super.onDestroyView()
    }

    override fun onSucceeded() {
        view!!.sensorDescription.setTextColor(resources.getColor(R.color.green, null))
        view!!.sensorDescription.text = getString(R.string.fingerprint_success)
        view!!.iconFingerprint.setImageResource(R.drawable.ic_fingerprint_success)

        view!!.sensorDescription.removeCallbacks(resetErrorTextRunnable)
        view!!.iconFingerprint.postDelayed({
            _authCallback.onNext(AuthCallback.OnAuth)
            isEnablingDismissed = false
        }, SUCCESS_DELAY_MILLIS)
    }

    override fun onFailed(error: String?) {
        showError(error ?: getString(R.string.fingerprint_not_recognized))
    }

    override fun onError(error: String?) {
        showError(error ?: getString(R.string.fingerprint_sensor_error))
        view!!.postDelayed(
            { _authCallback.onNext(AuthCallback.OnError) },
            ERROR_TIMEOUT_MILLIS
        )
    }

    private fun showError(error: CharSequence) {
        view!!.imageView.setImageResource(R.drawable.ic_fingerprint_fail)
        view!!.fingerprintStatus.text = error
        view!!.fingerprintStatus.setTextColor(resources.getColor(R.color.red, null))

        view!!.removeCallbacks(resetErrorTextRunnable)
        view!!.postDelayed(resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS)
    }

    private val resetErrorTextRunnable = Runnable {
        view!!.imageView.setImageResource(R.drawable.ic_fingerprint)
        view!!.fingerprintStatus.run {
            setTextColor(resources.getColor(R.color.gray_73_percent, null))
            text = getString(R.string.touch_fingerprint_sensor)
        }
    }
}
package mozilla.lockbox.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_fingerprint_dialog.view.*
import kotlinx.android.synthetic.main.onboarding_biometric_unlock.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.log
import mozilla.lockbox.presenter.OnboardingFingerprintAuthPresenter
import mozilla.lockbox.presenter.OnboardingFingerprintView
import mozilla.lockbox.model.FingerprintAuthCallback
import mozilla.lockbox.view.OnboardingFingerprintAuthFragment.AuthCallback.*

@ExperimentalCoroutinesApi
class OnboardingFingerprintAuthFragment : Fragment(), OnboardingFingerprintView {

    private val compositeDisposable = CompositeDisposable()

    private val _authCallback = PublishSubject.create<AuthCallback>()
    override val authCallback: Observable<AuthCallback> get() = _authCallback

    override val onDismiss: Observable<Unit>
        get() = view!!.skipOnboarding.clicks()

    private var isEnablingDismissed: Boolean = true

    companion object {
        private const val ERROR_TIMEOUT_MILLIS: Long = 1600
        private const val SUCCESS_DELAY_MILLIS: Long = 1300
    }

    sealed class AuthCallback : FingerprintAuthCallback {
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
        super.onViewCreated(view, savedInstanceState)


//        view.skipOnboarding.clicks()
//            .subscribe { dismiss() }
//            .addTo(compositeDisposable)
    }

    private fun dismiss() {}

    override fun onDestroyView() {
        compositeDisposable.clear()

        super.onDestroyView()
    }

    override fun onSucceeded() {
        log.info("ELISE - Fingerprint SUCCESS")
        view!!.sensorDescription.setTextColor(resources.getColor(R.color.green, null))
        view!!.sensorDescription.text = getString(R.string.fingerprint_success)
        view!!.iconFingerprint.setImageResource(R.drawable.ic_fingerprint_success)

        view!!.sensorDescription.removeCallbacks(resetErrorTextRunnable)
        view!!.iconFingerprint.postDelayed({
            _authCallback.onNext(OnAuth)
            isEnablingDismissed = false
        }, SUCCESS_DELAY_MILLIS)
    }

    override fun onFailed(error: String?) {
        log.info("ELISE - Fingerprint FAILURE")

        showError(error ?: getString(R.string.fingerprint_not_recognized))
    }

    override fun onError(error: String?) {
        log.info("ELISE - Fingerprint ERROR")

        showError(error ?: getString(R.string.fingerprint_sensor_error))
        view!!.postDelayed(
            { _authCallback.onNext(OnError) },
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
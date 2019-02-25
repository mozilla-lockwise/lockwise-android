package mozilla.lockbox.view

import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.log
import mozilla.lockbox.presenter.AutofillPresenter
import mozilla.lockbox.presenter.AutofillView
import java.lang.Exception

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class AutofillActivity : AppCompatActivity(), AutofillView {
    private lateinit var presenter: AutofillPresenter
    private val _unlockConfirmed = PublishSubject.create<Boolean>()

    override val context: Context = this

    companion object {
        private lateinit var responseBuilder: FillResponseBuilder

        fun getAuthIntentSender(context: Context, responseBuilder: FillResponseBuilder): IntentSender {
            this.responseBuilder = responseBuilder
            val intent = Intent(context, AutofillActivity::class.java)
            // add extras to the intent?
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        fun getSearchIntentSender(context: Context, responseBuilder: FillResponseBuilder): IntentSender {
            this.responseBuilder = responseBuilder
            val intent = Intent(context, AutofillActivity::class.java)
            // add extras to the intent?
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        private const val LOCK_REQUEST_CODE = 112
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = AutofillPresenter(this, responseBuilder)
        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun setFillResponseAndFinish(fillResponse: FillResponse?) {
        if (fillResponse == null) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse))
        }
        finish()
    }

    override fun showAuthDialog() {
        val dialogFragment = FingerprintAuthDialogFragment()
        val fragmentManager = supportFragmentManager
        try {
            dialogFragment.show(fragmentManager, dialogFragment.javaClass.name)
            dialogFragment.setupDialog(R.string.fingerprint_dialog_title)
        } catch (e: IllegalStateException) {
            log.error("Could not show dialog", e)
        }
    }

    override fun unlockFallback() {
        val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
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
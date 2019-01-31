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
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.log
import mozilla.lockbox.presenter.AuthPresenter
import mozilla.lockbox.presenter.AuthView
import mozilla.lockbox.support.ParsedStructure
import mozilla.lockbox.support.serverPasswordToDataset
import java.lang.Exception

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class AuthActivity : AppCompatActivity(), AuthView {
    private lateinit var presenter: AuthPresenter
    private val _unlockConfirmed = PublishSubject.create<Boolean>()

    companion object {
        private lateinit var webDomain: String
        private lateinit var parsedStructure: ParsedStructure

        fun getAuthIntentSender(context: Context, webDomain: String, parsedStructure: ParsedStructure): IntentSender {
            this.webDomain = webDomain
            this.parsedStructure = parsedStructure
            val intent = Intent(context, AuthActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        private const val LOCK_REQUEST_CODE = 112
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = AuthPresenter(this, webDomain)
        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun setUnlockResult(possibleValues: List<ServerPassword>) {
        if (possibleValues.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val builder = FillResponse.Builder()

        possibleValues
            .map { serverPasswordToDataset(this, parsedStructure, it) }
            .forEach { builder.addDataset(it) }

        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_AUTHENTICATION_RESULT, builder.build()))
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
package mozilla.lockbox.view

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.autofill.IntentBuilder
import mozilla.lockbox.presenter.AutofillRoutePresenter
import mozilla.lockbox.support.isDebug

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class AutofillRootActivity : AppCompatActivity() {
    private lateinit var presenter: AutofillRoutePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autofill)
        if (!isDebug()) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // pull responsebuilder & search status out of intent at a later iteration of this
        val responseBuilder = IntentBuilder.getResponseBuilder(intent)
        presenter = AutofillRoutePresenter(this, responseBuilder)
        presenter.onViewReady()
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    override fun onBackPressed() {
        if (!presenter.onBackPressed()) {
            super.onBackPressed()
        }
    }
}

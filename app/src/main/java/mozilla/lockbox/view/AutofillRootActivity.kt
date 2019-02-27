package mozilla.lockbox.view

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.R
import mozilla.lockbox.autofill.IntentBuilder
import mozilla.lockbox.presenter.AutofillRoutePresenter

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class AutofillRootActivity : AppCompatActivity() {
    private lateinit var presenter: AutofillRoutePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autofill)

        // pull responsebuilder & search status out of intent at a later iteration of this
        val responseBuilder = IntentBuilder.getResponseBuilder(intent)
        presenter = AutofillRoutePresenter(this, responseBuilder)
        presenter.onViewReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
package mozilla.lockbox

import android.annotation.TargetApi
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.store.DataStore

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared
) : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {}

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

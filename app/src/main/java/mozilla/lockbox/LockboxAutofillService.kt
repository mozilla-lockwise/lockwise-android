package mozilla.lockbox

import android.annotation.TargetApi
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.ParsedStructureBuilder

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    private var compositeDisposable = CompositeDisposable()

    override fun onDisconnected() {
        compositeDisposable.clear()
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val activityPackageName = structure.activityComponent.packageName
        if (this.packageName == activityPackageName) {
            callback.onSuccess(null)
            return
        }

        val parsedStructure = ParsedStructureBuilder(structure).build()
        if (parsedStructure.passwordId == null && parsedStructure.usernameId == null) {
            callback.onFailure(null)
            return
        }

        val packageName = parsedStructure.packageId ?: activityPackageName
        val webDomain = parsedStructure.webDomain

        val builder = FillResponseBuilder(parsedStructure, webDomain)

        dataStore.list
            .take(1)
            .subscribe { passwords ->
                val response = if (passwords.isEmpty()) {
                    builder.buildAuthenticationFillResponse(this)
                } else {
                    builder.buildFilteredFillResponse(this, passwords)
                }

                if (response == null) {
                    callback.onFailure("no logins found for this domain")
                } else {
                    callback.onSuccess(response)
                }
            }
            .addTo(compositeDisposable)
    }

    private fun domainFromPackage(packageName: String): String? {
        // naively assume that the `y` from `x.y.z`-style package name is the domain
        // untested as we will change this implementation with issue #375
        val domainRegex = Regex("^\\w+\\.(\\w+)\\..+")
        return domainRegex.find(packageName)?.groupValues?.get(1)
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}


package mozilla.lockbox

import android.annotation.TargetApi
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.widget.RemoteViews
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.ParsedStructure
import mozilla.lockbox.support.ParsedStructureBuilder
import mozilla.lockbox.support.serverPasswordToDataset
import mozilla.lockbox.view.AuthActivity

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
            callback.onFailure("couldn't find a username or password field")
            return
        }

        val packageName = parsedStructure.packageId ?: activityPackageName
        val webDomain = parsedStructure.webDomain

        // resolve the (webDomain || packageName) to a 1+publicsuffix
        val expectedDomain = when (webDomain) {
            null, "" -> pslSupport.fromPackageName(packageName)
            else -> pslSupport.fromWebDomain(webDomain)
        }

        // convert a list of ServerPasswords into a list of (psl+1, ServerPassword)
        val passwords = dataStore.list
            .take(1)
            .subscribe { passwords ->
                if (passwords.isEmpty()) {
                    auth(parsedStructure, webDomain, callback)
                } else {
                    val possibleValues = passwords.filter {
                        it.hostname.contains(webDomain, true)
                    }
                    val response = buildFillResponse(possibleValues, parsedStructure)
                    if (response == null) {
                        callback.onFailure("no logins found for this domain")
                    } else {
                        callback.onSuccess(response)
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    private fun auth(parsedStructure: ParsedStructure, webDomain:String, callback: FillCallback){
        val responseBuilder = FillResponse.Builder()

        val sender = AuthActivity.getAuthIntentSender(this, webDomain, parsedStructure)
        val autofillIds = arrayOf(parsedStructure.usernameId, parsedStructure.passwordId)

        val authPresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "requires authentication")
        }

        responseBuilder
            .setAuthentication(autofillIds, sender, authPresentation)

        return callback.onSuccess(responseBuilder.build())
    }

    private fun buildFillResponse(
        possibleValues: List<ServerPassword>,
        parsedStructure: ParsedStructure
    ): FillResponse? {
        if (possibleValues.isEmpty()) {
            return null
        }

        val builder = FillResponse.Builder()

        possibleValues
            .map { serverPasswordToDataset(this, parsedStructure, it) }
            .forEach { builder.addDataset(it) }

        return builder.build()
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

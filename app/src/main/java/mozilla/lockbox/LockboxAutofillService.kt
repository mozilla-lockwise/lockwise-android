package mozilla.lockbox

import android.annotation.TargetApi
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.ParsedStructure
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

    override fun onConnected() {
        // stupidly unlock every time :D
        dispatcher.dispatch(DataStoreAction.Unlock)
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val requestingPackage = domainFromPackage(structure.activityComponent.packageName)
        val parsedStructure = ParsedStructureBuilder(structure).build()

        if (parsedStructure.passwordId == null && parsedStructure.usernameId == null) {
            callback.onFailure("couldn't find a username or password field")
            return
        }

        if (requestingPackage == null) {
            callback.onFailure("unexpected package name structure")
            return
        }

        dataStore.list
            .filter { !it.isEmpty() }
            .take(1)
            .subscribe { passwords ->
                val possibleValues = passwords.filter {
                    it.hostname.contains(requestingPackage, true)
                }
                val response = buildFillResponse(possibleValues, parsedStructure)
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

    private fun buildFillResponse(
        possibleValues: List<ServerPassword>,
        parsedStructure: ParsedStructure
    ): FillResponse? {
        if (possibleValues.isEmpty()) {
            return null
        }

        val dataset = datasetForPossibleValues(possibleValues, parsedStructure)
        // future parts of this method include adding any authentication steps

        return FillResponse.Builder()
            .addDataset(dataset)
            .build()
    }

    private fun datasetForPossibleValues(
        possibleValues: List<ServerPassword>,
        parsedStructure: ParsedStructure
    ): Dataset {
        val datasetBuilder = Dataset.Builder()

        possibleValues.forEach { credential ->
            val usernamePresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            val passwordPresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            usernamePresentation.setTextViewText(android.R.id.text1, credential.username)
            passwordPresentation.setTextViewText(android.R.id.text1, getString(R.string.password_for, credential.username))

            parsedStructure.usernameId?.let {
                datasetBuilder.setValue(it, AutofillValue.forText(credential.username), usernamePresentation)
            }

            parsedStructure.passwordId?.let {
                datasetBuilder.setValue(it, AutofillValue.forText(credential.password), passwordPresentation)
            }
        }

        return datasetBuilder.build()
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

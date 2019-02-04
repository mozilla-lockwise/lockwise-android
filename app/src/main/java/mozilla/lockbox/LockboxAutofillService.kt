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
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.asMaybe
import mozilla.appservices.logins.ServerPassword
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.ParsedStructure
import mozilla.lockbox.support.ParsedStructureBuilder
import java.net.URI
import kotlin.coroutines.CoroutineContext

data class FillablePassword(
    val domain: String,
    val entry: ServerPassword
)

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    private var compositeDisposable = CompositeDisposable()
    private val exceptionHandler: CoroutineExceptionHandler
        get() = CoroutineExceptionHandler { _, e ->
            log.error(
                message = "Unexpected error occurred during PublicSuffixList usage",
                throwable = e
            )
        }

    private val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + exceptionHandler

    private val publicSuffixList = PublicSuffixList(this)


    override fun onDisconnected() {
        compositeDisposable.clear()
    }

    override fun onConnected() {
        // stupidly unlock every time :D
        dispatcher.dispatch(DataStoreAction.Unlock)
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val parsedStructure = ParsedStructureBuilder(structure).build()
        val packageName = parsedStructure.packageId ?: structure.activityComponent.packageName
        val webDomain = parsedStructure.webDomain ?: domainFromPackage(packageName)

        if (parsedStructure.passwordId == null && parsedStructure.usernameId == null) {
            callback.onFailure("couldn't find a username or password field")
            return
        }

        val domainObs = publicSuffixList.getPublicSuffixPlusOne(webDomain)
            .asMaybe(coroutineContext)
            .toSingle("")
            .toObservable()

        // convert a list of ServerPasswords into a list of (psl+1, username, password)
        val passwords = dataStore.list
            .take(1)
            .flatMap {
                Observable.merge(it.map {sp ->
                    val host = URI.create(sp.hostname).host
                    publicSuffixList.getPublicSuffixPlusOne(host)
                        .asMaybe(coroutineContext)
                        .toSingle("")
                        .toObservable()
                        .filter { !it.isEmpty() }
                        .map { domain ->
                            FillablePassword(domain, sp)
                        }
                })
            }
            .toList()
            .toObservable()

        Observables.combineLatest(domainObs, passwords)
            .subscribe {
                val expected = it.first
                if (it.first.isEmpty()) {
                    callback.onFailure("no web domain to match against")
                    return@subscribe
                }

                val possibleValues = it.second.filter {
                    it.domain.equals(expected, true)
                }.map { it.entry }
                val response = buildFillResponse(possibleValues, parsedStructure)
                if (response == null) {
                    callback.onFailure("no logins found for this domain")
                } else {
                    callback.onSuccess(response)
                }
            }
            .addTo(compositeDisposable)
    }

    private fun domainFromPackage(packageName: String): String {
        // naively assume package labels are split by `ASCII PERIOD (.)`
        return packageName.split(".").asReversed().joinToString(".")
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
            .map { serverPasswordToDataset(parsedStructure, it) }
            .forEach { builder.addDataset(it) }

        return builder.build()
    }

    private fun serverPasswordToDataset(parsedStructure: ParsedStructure, credential: ServerPassword): Dataset {
        val datasetBuilder = Dataset.Builder()
        val usernamePresentation = RemoteViews(packageName, R.layout.autofill_item)
        val passwordPresentation = RemoteViews(packageName, R.layout.autofill_item)
        usernamePresentation.setTextViewText(R.id.presentationText, credential.username)
        passwordPresentation.setTextViewText(R.id.presentationText, getString(R.string.password_for, credential.username))

        parsedStructure.usernameId?.let {
            datasetBuilder.setValue(it, AutofillValue.forText(credential.username), usernamePresentation)
        }

        parsedStructure.passwordId?.let {
            datasetBuilder.setValue(it, AutofillValue.forText(credential.password), passwordPresentation)
        }

        return datasetBuilder.build()
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

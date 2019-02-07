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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.ParsedStructure
import mozilla.lockbox.support.ParsedStructureBuilder
import mozilla.lockbox.support.PublicSuffix
import mozilla.lockbox.support.PublicSuffixSupport

data class FillablePassword(
    val domain: PublicSuffix,
    val entry: ServerPassword
)

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    private var compositeDisposable = CompositeDisposable()
    private val pslSupport = PublicSuffixSupport(
        PublicSuffixList(this)
    )

    override fun onDisconnected() {
        compositeDisposable.clear()
    }

    override fun onConnected() {
        // stupidly unlock every time :D
        dispatcher.dispatch(DataStoreAction.Unlock)
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
            .switchMap { serverPasswords ->
                val parsedPasswords = serverPasswords
                    .map { serverPwd ->
                        pslSupport.fromOrigin(serverPwd.hostname)
                            .map { FillablePassword(it, serverPwd) }
                    }
                val zipper: (Array<Any>) -> List<FillablePassword> = {
                    it.filter { it is FillablePassword }
                        .map { it as FillablePassword }
                }

                when (serverPasswords.size) {
                    0 -> Observable.just(emptyList<FillablePassword>())
                    else -> Observable.zipIterable(
                        parsedPasswords,
                        zipper,
                        false,
                        serverPasswords.size
                    )
                }
            }
            .take(1)

        Observables.combineLatest(expectedDomain, passwords)
            .subscribe(
                {
                    val expected = it.first
                    if (expected.isEmpty()) {
                        callback.onFailure("no web domain to match against")
                    } else {
                        val possibleValues = it.second
                            .filter { fillable ->
                                fillable.domain.isNotEmpty() && fillable.domain.matches(expected)
                            }
                            .map { fillable -> fillable.entry }
                        val response = buildFillResponse(possibleValues, parsedStructure)
                        if (response == null) {
                            callback.onFailure("no logins found for this domain")
                        } else {
                            log.debug("autofill searching response")
                            callback.onSuccess(response)
                        }
                    }
                }, {
                    val msg = "autofill searching unexpectedly failed"
                    log.error(msg, it)
                    callback.onFailure(msg)
                })
            .addTo(compositeDisposable)
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

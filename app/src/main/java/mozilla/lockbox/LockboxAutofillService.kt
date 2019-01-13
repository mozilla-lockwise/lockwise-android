package mozilla.lockbox

import android.R
import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.app.assist.AssistStructure.WindowNode
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View.AUTOFILL_HINT_PASSWORD
import android.view.View.AUTOFILL_HINT_USERNAME
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.model.titleFromHostname
import mozilla.lockbox.store.DataStore

data class ParsedStructure(
    val usernameId: AutofillId?,
    val passwordId: AutofillId?
)

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    var compositeDisposable = CompositeDisposable()

    override fun onDisconnected() {
        compositeDisposable.clear()
    }

    override fun onConnected() {
        // stupidly unlock every time :D
        dispatcher.dispatch(DataStoreAction.Unlock)
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val parsedStructure = traverseStructure(structure)
        val requestingPackage = domainFromPackage(structure.activityComponent.packageName)
        log.info("requestingPackage: $requestingPackage")

        dataStore.list
            .take(1)
            .subscribe { passwords ->
                val possibleValues = passwords.filter {
                    titleFromHostname(it.hostname).startsWith(requestingPackage, true)
                }

                val response = buildFillResponse(possibleValues, parsedStructure)
                callback.onSuccess(response)
            }
            .addTo(compositeDisposable)
    }

    private fun domainFromPackage(packageName: String): String {
        // todo: parse domain from package name
        // stupidly assume that the `x` from `w.x.y.z` is the domain?
        return "twitter"
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}

    private fun buildFillResponse(
        possibleValues: List<ServerPassword>,
        parsedStructure: ParsedStructure?
    ): FillResponse? {
        val structure = parsedStructure ?: return null
        val dataset = datasetForPossibleValues(possibleValues, structure)
        // future parts of this method include adding any authentication steps

        return FillResponse.Builder()
            .addDataset(dataset)
            .build()
    }

    private fun datasetForPossibleValues(
        possibleValues: List<ServerPassword>,
        parsedStructure: ParsedStructure
    ): Dataset {
        val builder = Dataset.Builder()

        possibleValues.forEach { credential ->
            val usernamePresentation = RemoteViews(packageName, R.layout.simple_list_item_1)
            val passwordPresentation = RemoteViews(packageName, R.layout.simple_list_item_1)
            usernamePresentation.setTextViewText(R.id.text1, credential.username)
            passwordPresentation.setTextViewText(R.id.text1, "Password for ${credential.password}")

            parsedStructure.passwordId?.let {
                builder.setValue(it, AutofillValue.forText(credential.password))
            }

            parsedStructure.usernameId?.let {
                builder.setValue(it, AutofillValue.forText(credential.username))
            }
        }

        return builder.build()
    }

    private fun traverseStructure(structure: AssistStructure): ParsedStructure? {
        val usernameId = getUsernameId(structure)
        val passwordId = getPasswordId(structure)

        return ParsedStructure(usernameId, passwordId)
    }

    private fun getUsernameId(structure: AssistStructure): AutofillId? {
        return getAutofillIdForHint(AUTOFILL_HINT_USERNAME, structure)
    }

    private fun getPasswordId(structure: AssistStructure): AutofillId? {
        return getAutofillIdForHint(AUTOFILL_HINT_PASSWORD, structure)
    }

    private fun getAutofillIdForHint(hint: String, structure: AssistStructure): AutofillId? {
        val windowNodes: List<WindowNode> =
            structure.run {
                (0 until windowNodeCount).map { getWindowNodeAt(it) }
            }

        return windowNodes
            .map { windowNode: WindowNode ->
                val viewNode: ViewNode? = windowNode.rootViewNode
                traverseNode(hint, viewNode)
            }
            .first { it != null }
    }

    private fun traverseNode(hint: String, viewNode: ViewNode?): AutofillId? {
        viewNode?.autofillHints?.toList()?.let {
            if (it.contains(hint)) {
                return viewNode.autofillId // we're done
            }
        }

        return viewNode?.run {
            (0 until childCount).map { getChildAt(it) }
        }?.map {
            traverseNode(hint , it)
        }?.first { it != null }
    }
}

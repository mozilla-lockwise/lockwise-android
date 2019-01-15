package mozilla.lockbox

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS
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
        val parsedStructure = parseStructure(structure)
        val requestingPackage = domainFromPackage(structure.activityComponent.packageName)
        log.info("requesting package: $requestingPackage")
        log.info("parsed structure: $parsedStructure")

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

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}

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
        val builder = Dataset.Builder()

        possibleValues.forEach { credential ->
            val usernamePresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            val passwordPresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            usernamePresentation.setTextViewText(android.R.id.text1, credential.username)
            passwordPresentation.setTextViewText(android.R.id.text1, "Password for ${credential.username}")

            parsedStructure.usernameId?.let {
                builder.setValue(it, AutofillValue.forText(credential.username), usernamePresentation)
            }

            parsedStructure.passwordId?.let {
                builder.setValue(it, AutofillValue.forText(credential.password), passwordPresentation)
            }
        }

        return builder.build()
    }

    private fun parseStructure(structure: AssistStructure): ParsedStructure {
        val usernameId = getUsernameId(structure)
        val passwordId = getPasswordId(structure)

        return ParsedStructure(usernameId, passwordId)
    }

    private fun getUsernameId(structure: AssistStructure): AutofillId? {
        // how do we localize the "email" and "username"?
        return getAutofillIdForKeywords(structure, listOf(
            AUTOFILL_HINT_USERNAME,
            AUTOFILL_HINT_EMAIL_ADDRESS,
            "email",
            "username",
            "user name"
        ))
    }

    private fun getPasswordId(structure: AssistStructure): AutofillId? {
        // similar l10n question for password
        return getAutofillIdForKeywords(structure, listOf(AUTOFILL_HINT_PASSWORD, "password"))
    }

    private fun getAutofillIdForKeywords(structure: AssistStructure, keywords: List<String>): AutofillId? {
        return try {
            structure
                .run { (0 until windowNodeCount).map { getWindowNodeAt(it) } }
                .map { traverseNode(it.rootViewNode, keywords) }
                .first { it != null }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun traverseNode(viewNode: ViewNode?, keywords: List<String>): AutofillId? {
        // this method is getting less & less performant
        val node = viewNode ?: return null

        if (containsKeywords(node, keywords) && node.autofillId != null) {
            return node.autofillId
        }

        val childNodes = node
            .run { (0 until childCount).map { getChildAt(it) } }

        // check for consecutive views with keywords followed by possible fill locations
        for (i in 1..(childNodes.size - 1)) {
            val prevNode = childNodes[i - 1]
            val currentNode = childNodes[i]
            if (containsKeywords(prevNode, keywords) && currentNode.autofillId != null) {
                return currentNode.autofillId
            }
        }

        // if neither of the above succeeds, go deeper....
        return try {
            childNodes
                .map { traverseNode(it, keywords) }
                .first { it != null }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun containsKeywords(node: ViewNode, keywords: List<String>): Boolean {
        val autofillHints = node.autofillHints?.toList() ?: emptyList()
        var hints = listOf(node.hint) + listOf(node.text) + autofillHints

        hints = hints.filterNotNull()

        keywords.forEach { keyword ->
            hints.forEach { hint ->
                if (hint.contains(keyword, true)) {
                    return true
                }
            }
        }

        return false
    }
}

package mozilla.lockbox

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
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.store.DataStore

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared
) : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure

        val requestingPackage = structure.activityComponent.packageName
        log.info(requestingPackage)

        val hints = traverseStructure(structure)
        hints.forEach {
            log.info(it)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}

    private fun traverseStructure(structure: AssistStructure): List<String> {
        val windowNodes: List<WindowNode> =
            structure.run {
                (0 until windowNodeCount).map { getWindowNodeAt(it) }
            }

        return windowNodes.map { windowNode: WindowNode ->
            val viewNode: ViewNode? = windowNode.rootViewNode
            traverseNode(viewNode)
        }.flatten()
    }

    private fun traverseNode(viewNode: ViewNode?): List<String> {
        val topLevelHints = viewNode?.autofillHints?.toList() ?: emptyList()

        val childNodeHints =
            viewNode?.run {
                (0 until childCount).map { getChildAt(it) }
            }?.map {
                traverseNode(it)
            }?.flatten()
                ?: emptyList()

        return topLevelHints + childNodeHints
    }

}

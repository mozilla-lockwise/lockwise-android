/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.autofill.ClientParser
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.autofill.ParsedStructure
import mozilla.lockbox.autofill.ParsedStructureBuilder
import mozilla.lockbox.autofill.ViewNodeNavigator
import mozilla.lockbox.extensions.dump
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AutofillStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.TelemetryStore
import mozilla.lockbox.support.Optional
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.isDebug
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.model.AutofillDataViewModel
import mozilla.lockbox.model.AutofillItemViewModel

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared,
    private val telemetryStore: TelemetryStore = TelemetryStore.shared,
    private val autofillStore: AutofillStore = AutofillStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    private var compositeDisposable = CompositeDisposable()
    private val pslSupport = PublicSuffixSupport.shared
    private lateinit var parsedStructure: ParsedStructure

    override fun onConnected() {
        dispatcher.dispatch(LifecycleAction.AutofillStart)
        telemetryStore.injectContext(this)
    }

    override fun onDisconnected() {
        dispatcher.dispatch(LifecycleAction.AutofillEnd)
        compositeDisposable.clear()
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val structure = request.fillContexts.last().structure
        val activityPackageName = structure.activityComponent.packageName
        if (this.packageName == activityPackageName) {
            callback.onSuccess(null)
            return
        }

        val nodeNavigator = ViewNodeNavigator(structure, activityPackageName)
        parsedStructure = ParsedStructureBuilder(nodeNavigator).build() as ParsedStructure

        if (parsedStructure.passwordId == null && parsedStructure.usernameId == null) {
            if (isDebug()) {
                val xml = structure.getWindowNodeAt(0).rootViewNode.dump()
                log.debug("Autofilling failed for:\n$xml")
            }
            callback.onSuccess(null)
            return
        }

        val builder = FillResponseBuilder(parsedStructure)

        // When locked, then the list will be empty.
        // We have to do it as an observable, as looking up PSL is all async.
        val filteredPasswords = builder.asyncFilter(pslSupport, dataStore.list)

        // If the data store is locked, then authenticate
        // If the data store is unlocked, with matching, then filtered response.
        // If the data store is unlocked with no matching, then send to list?
        Observables.combineLatest(dataStore.state, filteredPasswords)
            .take(1)
            .map { latest ->
                val state = latest.first
                when (state) {
                    is DataStore.State.Locked -> AutofillAction.Authenticate
                    is DataStore.State.Unlocked -> AutofillAction.CompleteMultiple(latest.second)
                    is DataStore.State.Unprepared -> AutofillAction.Cancel // we might consider onboarding here.
                    is DataStore.State.Errored -> AutofillAction.Error(state.error)
                }
            }
            .onErrorReturnItem(AutofillAction.SearchFallback)
            .subscribe(dispatcher::dispatch) {
                log.error(throwable = it)
            }
            .addTo(compositeDisposable)

        autofillStore.autofillActions
            .take(1)
            .map {
                val appName = this.getString(R.string.app_name)
                when (it) {
                    is AutofillAction.Complete -> builder.buildFilteredFillResponse(this, listOf(it.login)).asOptional()
                    is AutofillAction.CompleteMultiple -> (builder.buildFilteredFillResponse(this, it.logins)
                        ?: builder.buildFallbackFillResponse(this)).asOptional()
                    is AutofillAction.SearchFallback -> builder.buildFallbackFillResponse(this).asOptional()
                    is AutofillAction.Authenticate -> builder.buildAuthenticationFillResponse(this).asOptional()
                    is AutofillAction.Cancel -> Optional(null)
                    is AutofillAction.Error -> {
                        callback.onFailure(getString(R.string.autofill_error_toast, appName, it.error.localizedMessage))
                        null
                    }
                }.asOptional()
            }
            .filterNotNull()
            .subscribe({
                // either call onSuccess (is there a failure path? or is onSuccess like "complete"?)
                // or onSaveRequest

                callback.onSuccess(it.value)
            }, {
                log.error(throwable = it)
            })
            .addTo(compositeDisposable)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        log.info("Save request values: $request")

        val clientState = request.clientState
        log.info("Bundle: $clientState")

        val structure = request.fillContexts.last().structure
        val activityPackageName = structure.activityComponent.packageName
        if (this.packageName == activityPackageName) {
            callback.onSuccess(null)
            return
        }

        // get ViewNode
        val nodeNavigator = ViewNodeNavigator(structure, activityPackageName)
        parsedStructure = ParsedStructureBuilder(nodeNavigator).build() as ParsedStructure

        // get the ids we already know
        val usernameId: AutofillId? = clientState?.getParcelable("usernameId")
        val passwordId: AutofillId? = clientState?.getParcelable("passwordId")

        // find the matching viewnodes
        val usernameNode = findNodeByAutofillId(structure, usernameId)
        val passwordNode = findNodeByAutofillId(structure, passwordId)

        // find the username and password values
        val username = usernameNode.autofillValue?.textValue.toString()
        val password = passwordNode.autofillValue?.textValue.toString()

        log.info("ParsedStructure: $parsedStructure")

        // questions:
        // what do we want to use as the autofill id?
        val autofillItem = AutofillItemViewModel(
            autofillId = activityPackageName,
            hostName = parsedStructure.webDomain ?: "",
            username = username,
            password = password
        )

        dispatcher.dispatch(DataStoreAction.Add(autofillItem))
        callback.onSuccess()
    }

    // better way to do this...
    private fun findNodeByAutofillId(structure: AssistStructure, id: AutofillId?): AssistStructure.ViewNode {
        if(id == null) {
            log.info("AutofillId is null")
        }

        val nodes = structure.windowNodeCount

        var index = 0
        var node: AssistStructure.ViewNode? = null
        while (index < nodes && node == null) {
            node = parseNode(structure.getWindowNodeAt(index).rootViewNode, id)
            index++
        }

        return node!!
    }

    private fun parseNode(root: AssistStructure.ViewNode, id: AutofillId?): AssistStructure.ViewNode {
        if (root.autofillId == id) {
            return root
        }
        else {
            val childrenSize = root.childCount
            for (i in 0 until childrenSize) {
                parseNode(root.getChildAt(i))
            }
        }
    }
}
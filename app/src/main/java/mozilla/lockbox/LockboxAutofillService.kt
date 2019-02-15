/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox

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
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.autofill.ViewNodeNavigator
import mozilla.lockbox.extensions.dump
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.autofill.ParsedStructureData
import mozilla.lockbox.autofill.ParsedStructureBuilder
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.isDebug

typealias ParsedStructure = ParsedStructureData<AutofillId>

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    val dataStore: DataStore = DataStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    private var compositeDisposable = CompositeDisposable()
    private val pslSupport = PublicSuffixSupport.shared

    override fun onConnected() {
        dispatcher.dispatch(LifecycleAction.AutofillStart)
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
        val parsedStructure = ParsedStructureBuilder(nodeNavigator).build()

        if (parsedStructure.passwordId == null && parsedStructure.usernameId == null) {
            if (isDebug()) {
                val xml = structure.getWindowNodeAt(0).rootViewNode.dump()
                log.debug("Autofilling failed for:\n$xml")
            }
            callback.onFailure(null)
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
                when (latest.first) {
                    DataStore.State.Locked -> builder.buildAuthenticationFillResponse(this)
                    DataStore.State.Unlocked -> {
                        builder.buildFilteredFillResponse(this, latest.second)
                        ?: builder.buildFallbackFillResponse(this)
                    }
                    DataStore.State.Unprepared -> null // we might consider onboarding here.
                    else -> null
                }.asOptional()
            }
            .subscribe {
                if (it.value != null) {
                    callback.onSuccess(it.value)
                } else {
                    callback.onFailure(null)
                }
            }
            .addTo(compositeDisposable)
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

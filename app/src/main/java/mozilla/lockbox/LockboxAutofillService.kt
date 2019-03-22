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
import androidx.annotation.RequiresApi
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.LifecycleAction
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
        val parsedStructure = ParsedStructureBuilder(nodeNavigator).build() as ParsedStructure

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
            .map {
                when (it) {
                    is AutofillAction.Complete -> builder.buildFilteredFillResponse(this, listOf(it.login)).asOptional()
                    is AutofillAction.CompleteMultiple -> (builder.buildFilteredFillResponse(this, it.logins)
                        ?: builder.buildFallbackFillResponse(this)).asOptional()
                    is AutofillAction.SearchFallback -> builder.buildFallbackFillResponse(this).asOptional()
                    is AutofillAction.Authenticate -> builder.buildAuthenticationFillResponse(this).asOptional()
                    is AutofillAction.Cancel -> Optional(null)
                    is AutofillAction.Error -> {
                        callback.onFailure(getString(R.string.autofill_error_toast, it.error.localizedMessage))
                        null
                    }
                }.asOptional()
            }
            .filterNotNull()
            .subscribe {
                callback.onSuccess(it.value)
            }
            .addTo(compositeDisposable)
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

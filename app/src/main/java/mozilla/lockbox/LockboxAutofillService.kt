/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox

import android.annotation.TargetApi
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.support.ParsedStructureBuilder
import mozilla.lockbox.support.asOptional

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
            callback.onFailure(null)
            return
        }
        
        val webDomain = parsedStructure.webDomain
        if (webDomain == null) {
            callback.onFailure(getString(R.string.autofill_error_no_hostname))
            return
        }

        val builder = FillResponseBuilder(parsedStructure, webDomain)

        // If the data store is locked, then authenticate
        // If the data store is unlocked, with matching, then filtered response.
        // If the data store is unlocked with no matching, then send to list?
        Observables.combineLatest(dataStore.state, dataStore.list)
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

    private fun domainFromPackage(packageName: String): String? {
        // naively assume that the `y` from `x.y.z`-style package name is the domain
        // untested as we will change this implementation with issue #375
        val domainRegex = Regex("^\\w+\\.(\\w+)\\..+")
        return domainRegex.find(packageName)?.groupValues?.get(1)
    }

    // to be implemented in issue #217
    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {}
}

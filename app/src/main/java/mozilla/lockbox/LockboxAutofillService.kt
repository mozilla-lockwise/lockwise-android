/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillEventHistory
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import androidx.annotation.RequiresApi
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.autofill.AutofillTextValueBuilder
import mozilla.lockbox.autofill.FillResponseBuilder
import mozilla.lockbox.autofill.ParsedStructure
import mozilla.lockbox.autofill.ParsedStructureBuilder
import mozilla.lockbox.autofill.ViewNodeNavigator
import mozilla.lockbox.extensions.dump
import mozilla.lockbox.extensions.filterNotNull
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.store.AccountStore
import mozilla.lockbox.store.AutofillStore
import mozilla.lockbox.store.DataStore
import mozilla.lockbox.store.GleanTelemetryStore
import mozilla.lockbox.store.SettingStore
import mozilla.lockbox.store.TelemetryStore
import mozilla.lockbox.support.FxASyncDataStoreSupport
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FeatureFlags
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.SecurePreferences
import mozilla.lockbox.support.asOptional
import mozilla.lockbox.support.isDebug

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class LockboxAutofillService(
    private val accountStore: AccountStore = AccountStore.shared,
    private val dataStore: DataStore = DataStore.shared,
    private val settingStore: SettingStore = SettingStore.shared,
    private val securePreferences: SecurePreferences = SecurePreferences.shared,
    private val fxaSupport: FxASyncDataStoreSupport = FxASyncDataStoreSupport.shared,
    private val gleanTelemetryStore: GleanTelemetryStore = GleanTelemetryStore.shared,
    private val autofillStore: AutofillStore = AutofillStore.shared,
    val dispatcher: Dispatcher = Dispatcher.shared
) : AutofillService() {

    private var compositeDisposable = CompositeDisposable()
    private val pslSupport = PublicSuffixSupport.shared

    private var isRunning = false

    override fun onConnected() {
        isRunning = false
    }

    override fun onDisconnected() {
        if (isRunning) {
            isRunning = false
            dispatcher.dispatch(LifecycleAction.AutofillEnd)
        }
    }

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        touchRecentlyUsedDatasets()

        val structure = request.fillContexts.last().structure
        val activityPackageName = structure.activityComponent.packageName
        if (this.packageName == activityPackageName) {
            callback.onFailure(null)
            return
        }

        val nodeNavigator = ViewNodeNavigator(structure, activityPackageName)
        val parsedStructure = ParsedStructureBuilder(nodeNavigator).build() as ParsedStructure

        if (parsedStructure.passwordId == null && parsedStructure.usernameId == null) {
            if (isDebug()) {
                val xml = structure.getWindowNodeAt(0).rootViewNode.dump()
                log.debug("Autofilling $activityPackageName failed for:\n$xml")
            }
            callback.onFailure(null)
            return
        }

        initializeService()

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
                    is DataStore.State.Errored -> null
                }.asOptional()
            }
            .filterNotNull()
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
                    is AutofillAction.Complete -> builder.buildFilteredFillResponse(this, listOf(it.login))
                    is AutofillAction.CompleteMultiple -> (builder.buildFilteredFillResponse(this, it.logins)
                        ?: builder.buildFallbackFillResponse(this))
                    is AutofillAction.SearchFallback -> builder.buildFallbackFillResponse(this)
                    is AutofillAction.Authenticate -> builder.buildAuthenticationFillResponse(this)
                    is AutofillAction.Cancel -> null
                    is AutofillAction.Error -> {
                        callback.onFailure(getString(R.string.autofill_error_toast, appName, it.error.localizedMessage))
                        null
                    }
                }.asOptional()
            }
            .filterNotNull()
            .doOnComplete {
                compositeDisposable.clear()
            }
            .subscribe({
                callback.onSuccess(it)
            }, {
                log.error(throwable = it)
            })
            .addTo(compositeDisposable)
    }

    private fun touchRecentlyUsedDatasets() {
        val selectedDatasetIds = fillEventHistory?.events?.let { list ->
            list.filter { it.type == FillEventHistory.Event.TYPE_DATASET_SELECTED }
                .mapNotNull { it.datasetId }
        }
        if (selectedDatasetIds?.isEmpty() != false) {
            return
        }
        initializeService()
        selectedDatasetIds
            .map { DataStoreAction.AutofillTouch(it) }
            .forEach(dispatcher::dispatch)
    }

    private fun initializeService() {
        if (isRunning) {
            // we might have already been called when logging
            // a previously chosen dataset.
            return
        }
        isRunning = true

        val contextInjectables = listOfNotNull(
            settingStore,
            gleanTelemetryStore,
            if (FeatureFlags.INCLUDE_DEPRECATED_TELEMETRY) TelemetryStore.shared else null,
            securePreferences,
            accountStore,
            fxaSupport
        )

        contextInjectables.forEach {
            it.injectContext(this)
        }
        dispatcher.dispatch(LifecycleAction.AutofillStart)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val parsedStructure = request.clientState?.let {
            it.classLoader = ParsedStructure::class.java.classLoader
            it.getParcelable<ParsedStructure>(Constant.Key.parsedStructure)
        } ?: return callback.onFailure("Bundle missing")

        val structure = request.fillContexts.last().structure
        val nodeNavigator = ViewNodeNavigator(structure, parsedStructure.packageName)

        val autofillItem = AutofillTextValueBuilder(parsedStructure, nodeNavigator).build()

        // The SaveInfo we have fillled in means that we'll only get here if there's a username and password.
        // We can safely bail knowing we'll never need to here.
        val capturedUsername = autofillItem.username
        val capturedPassword = autofillItem.password ?: return callback.onFailure("Password missing")

        // According to the AsyncLoginsStorage docs:
        // "If login has an empty id field, then a GUID will be generated automatically."
        val emptyId = ""

        val pslSuffix =
            parsedStructure.webDomain?.let { pslSupport.fromWebDomain(it) }
            ?: pslSupport.fromPackageName(parsedStructure.packageName)

        pslSuffix.take(1)
            .map { suffix ->
                val domain = "https://${suffix.fullDomain}"
                val webDomain = parsedStructure.webDomain?.let { "https://$it" }
                ServerPassword(
                    id = emptyId,
                    hostname = domain,
                    username = capturedUsername,
                    password = capturedPassword,
                    formSubmitURL = webDomain ?: domain
                )
            }
            .doOnComplete {
                compositeDisposable.clear()
            }
            .subscribe {
                dispatcher.dispatch(DataStoreAction.AutofillCapture(it))
                callback.onSuccess()
            }
            .addTo(compositeDisposable)
    }
}

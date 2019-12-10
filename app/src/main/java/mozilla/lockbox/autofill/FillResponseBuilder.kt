package mozilla.lockbox.autofill

import android.annotation.TargetApi
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.model.titleFromHostname
import mozilla.lockbox.support.Constant
import mozilla.lockbox.support.FeatureFlags
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.filter

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
open class FillResponseBuilder(
    internal val parsedStructure: ParsedStructure
) {

    fun buildAuthenticationFillResponse(context: Context): FillResponse {
        val responseBuilder = FillResponse.Builder()

        val presentation = RemoteViews(context.packageName, R.layout.autofill_cta_presentation)
        val appName = context.getString(R.string.app_name)
        val authenticationText = context.getString(R.string.autofill_authenticate_cta, appName)
        presentation.setTextViewText(R.id.autofill_cta, authenticationText)

        val sender = IntentBuilder.getAuthIntentSender(context, this)

        responseBuilder.setAuthentication(parsedStructure.autofillIds, sender, presentation)
        setupSaveInfo(responseBuilder)

        return responseBuilder.build()
    }

    private fun buildSaveInfo(): SaveInfo {
        val builder = SaveInfo.Builder(
            parsedStructure.saveInfoMask,
            parsedStructure.autofillIds
        )

        return builder.build()
    }

    fun buildFallbackFillResponse(context: Context): FillResponse? {
        // See https://github.com/mozilla-lockwise/lockwise-android/issues/421
        val builder = FillResponse.Builder()
        addSearchFallback(context) { sender, presentation ->
            builder.setAuthentication(parsedStructure.autofillIds, sender, presentation)
        }
        setupSaveInfo(builder)
        return builder.build()
    }

    private fun addSearchFallback(
        context: Context,
        presentationAdder: (IntentSender, RemoteViews) -> Unit
    ) {
        val presentation = RemoteViews(context.packageName, R.layout.autofill_cta_presentation).apply {
            val appName = context.getString(R.string.app_name)
            setTextViewText(R.id.autofill_cta, context.getString(R.string.autofill_search_cta, appName))
        }

        // See https://github.com/mozilla-lockwise/lockwise-android/issues/421
        val sender = IntentBuilder.getSearchIntentSender(context, this)
        presentationAdder(sender, presentation)
    }

    open fun buildFilteredFillResponse(context: Context, filteredPasswords: List<ServerPassword>): FillResponse? {
        if (filteredPasswords.isEmpty()) {
            return null
        }

        val builder = FillResponse.Builder()

        filteredPasswords
            .map { serverPasswordToDataset(context, it) }
            .map(builder::addDataset)

        // no clickable footer is possible.
        addSearchFallback(context) { sender, presentation ->
            val datasetBuilder = Dataset.Builder()

            parsedStructure.autofillIds.forEach { id ->
                datasetBuilder.setValue(id, null, presentation)
            }

            datasetBuilder.setAuthentication(sender)

            builder.addDataset(datasetBuilder.build())
        }

        setupSaveInfo(builder)
        return builder.build()
    }

    private fun setupSaveInfo(builder: FillResponse.Builder) {
        if (FeatureFlags.AUTOFILL_CAPTURE) {
            builder.setSaveInfo(buildSaveInfo())

            val clientState = Bundle()
            clientState.putParcelable(Constant.Key.parsedStructure, parsedStructure)
            builder.setClientState(clientState)
        }
    }

    private fun serverPasswordToDataset(
        context: Context,
        credential: ServerPassword
    ): Dataset {
        val datasetBuilder = Dataset.Builder()

        val title = titleFromHostname(credential.hostname)
        val username = credential.username ?: context.getString(R.string.no_username)

        datasetBuilder.setId(credential.id)

        parsedStructure.usernameId?.let { id ->
            val presentation = RemoteViews(context.packageName, R.layout.autofill_item)
                .apply {
                    setTextViewText(R.id.autofillValue, username)
                    setTextViewText(R.id.hostname, title)
                }

            datasetBuilder.setValue(
                id,
                credential.username?.let { AutofillValue.forText(it) },
                presentation
            )
        }

        parsedStructure.passwordId?.let { id ->
            val presentation = RemoteViews(context.packageName, R.layout.autofill_item)
                .apply {
                    setTextViewText(R.id.autofillValue, context.getString(R.string.password_for, username))
                    setTextViewText(R.id.hostname, title)
                }

            datasetBuilder.setValue(
                id,
                AutofillValue.forText(credential.password),
                presentation
            )
        }

        return datasetBuilder.build()
    }

    open fun asyncFilter(pslSupport: PublicSuffixSupport, list: Observable<List<ServerPassword>>) =
        list.take(1).filter(pslSupport, parsedStructure.webDomain, parsedStructure.packageName)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FillResponseBuilder) return false

        if (parsedStructure != other.parsedStructure) return false

        return true
    }

    override fun hashCode(): Int {
        return parsedStructure.hashCode()
    }
}

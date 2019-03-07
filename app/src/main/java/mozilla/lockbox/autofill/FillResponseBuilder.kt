package mozilla.lockbox.autofill

import android.annotation.TargetApi
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.model.titleFromHostname
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
        presentation.setTextViewText(R.id.autofill_cta, context.getString(R.string.autofill_authenticate_cta))

        val sender = IntentBuilder.getAuthIntentSender(context, this)
        responseBuilder.setAuthentication(autofillIds(), sender, presentation)

        return responseBuilder.build()
    }

    private fun autofillIds(): Array<AutofillId> {
        return arrayOf(parsedStructure.usernameId, parsedStructure.passwordId)
            .filter { it != null }
            .map { it!! }
            .toTypedArray()
    }

    fun buildFallbackFillResponse(context: Context): FillResponse? {
        // See https://github.com/mozilla-lockbox/lockbox-android/issues/421
        val builder = FillResponse.Builder()
        addSearchFallback(context) { sender, presentation ->
            builder.setAuthentication(autofillIds(), sender, presentation)
        }
        return builder.build()
    }

    private fun addSearchFallback(
        context: Context,
        presentationAdder: (IntentSender, RemoteViews) -> Unit
    ) {
        val presentation = RemoteViews(context.packageName, R.layout.autofill_cta_presentation).apply {
            setTextViewText(R.id.autofill_cta, context.getString(R.string.autofill_search_cta))
        }

        // See https://github.com/mozilla-lockbox/lockbox-android/issues/421
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

            autofillIds().forEach { id ->
                datasetBuilder.setValue(id, null, presentation)
            }

            datasetBuilder.setAuthentication(sender)

            builder.addDataset(datasetBuilder.build())
        }

        return builder.build()
    }

    private fun serverPasswordToDataset(
        context: Context,
        credential: ServerPassword
    ): Dataset {
        val datasetBuilder = Dataset.Builder()

        val title = titleFromHostname(credential.hostname)

        parsedStructure.usernameId?.let {
            val presentation = RemoteViews(context.packageName, R.layout.autofill_item)
                .apply {
                    setTextViewText(R.id.autofillValue, credential.username)
                    setTextViewText(R.id.hostname, title)
                }

            datasetBuilder.setValue(
                it,
                AutofillValue.forText(credential.username),
                presentation
            )
        }

        parsedStructure.passwordId?.let {
            val presentation = RemoteViews(context.packageName, R.layout.autofill_item)
                .apply {
                    setTextViewText(R.id.autofillValue, context.getString(R.string.password_for, credential.username))
                    setTextViewText(R.id.hostname, title)
                }

            datasetBuilder.setValue(
                it,
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

package mozilla.lockbox.autofill

import android.annotation.TargetApi
import android.content.Context
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
import mozilla.lockbox.support.PublicSuffixSupport
import mozilla.lockbox.support.filter

@TargetApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
open class FillResponseBuilder(
    internal val parsedStructure: ParsedStructure
) {
    fun buildAuthenticationFillResponse(context: Context): FillResponse {
        val responseBuilder = FillResponse.Builder()

        val authPresentation = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            val searchFirefoxLockbox = context.resources.getString(R.string.autofill_authenticate_cta)
            setTextViewText(R.id.searchLockboxText, searchFirefoxLockbox)
        }

        val sender = IntentBuilder.getAuthIntentSender(context, this)
        responseBuilder.setAuthentication(autofillIds(), sender, authPresentation)

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
        addSearchFallback(context, builder)
        return builder.build()
    }

    private fun addSearchFallback(context: Context, builder: FillResponse.Builder) {
        val searchPresentation = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            val searchFirefoxLockbox = context.resources.getString(R.string.autofill_search_cta)
            setTextViewText(R.id.searchLockboxText, searchFirefoxLockbox)
        }
        // See https://github.com/mozilla-lockbox/lockbox-android/issues/421
        val sender = IntentBuilder.getSearchIntentSender(context, this)
        builder.setAuthentication(autofillIds(), sender, searchPresentation)
    }

    open fun buildFilteredFillResponse(context: Context, filteredPasswords: List<ServerPassword>): FillResponse? {

        if (filteredPasswords.isEmpty()) {
            return null
        }

        val builder = FillResponse.Builder()

        filteredPasswords
            .map { serverPasswordToDataset(context, it) }
            .forEach { builder.addDataset(it) }

        // no clickable footer is possible.

        return builder.build()
    }

    private fun serverPasswordToDataset(
        context: Context,
        credential: ServerPassword
    ): Dataset {
        val datasetBuilder = Dataset.Builder()
        val usernamePresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
        val passwordPresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
        // sets the autofill_item text to be a username if present
        usernamePresentation.setTextViewText(
            mozilla.lockbox.R.id.searchLockboxText,
            credential.username)
        // sets the autofill_item text to be a password if present
        passwordPresentation.setTextViewText(
            mozilla.lockbox.R.id.searchLockboxText,
            context.getString(mozilla.lockbox.R.string.password_for, credential.username)
        )

        parsedStructure.usernameId?.let {
            datasetBuilder.setValue(it,
                AutofillValue.forText(credential.username), usernamePresentation)
        }

        parsedStructure.passwordId?.let {
            datasetBuilder.setValue(it,
                AutofillValue.forText(credential.password), passwordPresentation)
        }

        // is this building a concatinated length of autofill_items?
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

package mozilla.lockbox.autofill

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
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

        val authPresentation = getAuthPresentation(context, R.layout.autofill_item)

        val sender = IntentBuilder.getAuthIntentSender(context, this)
        responseBuilder.setAuthentication(autofillIds(), sender, authPresentation)
        addSearchFallback(context, responseBuilder)
        return responseBuilder.build()
    }

    private fun getAuthPresentation(context: Context, layoutId: Int) : RemoteViews {
        return RemoteViews(context.packageName, layoutId).apply {
            val searchFirefoxLockbox = context.resources.getString(R.string.autofill_authenticate_cta)
            setTextViewText(R.id.searchLockboxText, searchFirefoxLockbox)
        }
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
        val searchPresentation = getAuthPresentation(context, R.layout.autofill_search)

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
            .forEach {
                val title = titleFromHostname(it.second!!)
                val headerView = RemoteViews(context.packageName, R.layout.autofill_item_title)
                    .apply {
                        setTextViewText(R.id.hostname, title)
                    }
                val footerView = RemoteViews(context.packageName, R.layout.autofill_search)
                    .apply {
                        setTextViewText(R.id.searchLockboxText, context.getString(R.string.autofill_search_cta))
//                        setImageViewResource(R.id.searchLockboxText, R.drawable.ic_search_dark)
                    }

                builder.setHeader(headerView)
                builder.setFooter(footerView)
                builder.addDataset(it.first)
            }

        // no clickable footer is possible.

        return builder.build()
    }

    private fun serverPasswordToDataset(
        context: Context,
        credential: ServerPassword
    ): Pair<Dataset, String?> {
        val datasetBuilder = Dataset.Builder()

        val hostnamePresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item_title)
        val usernamePresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
        val passwordPresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)

        // sets the autofill_item_title text to be a username if present
        hostnamePresentation.setTextViewText(
            mozilla.lockbox.R.id.hostname,
            credential.hostname)

        // sets the autofill_item_title text to be a username if present
        usernamePresentation.setTextViewText(
            mozilla.lockbox.R.id.autofillValue,
            credential.username)

        // sets the autofill_item_title text to be a password if present
        passwordPresentation.setTextViewText(
            mozilla.lockbox.R.id.autofillValue,
            context.getString(mozilla.lockbox.R.string.password_for, credential.password)
        )

        parsedStructure.webDomain?.let {
            datasetBuilder.setId(it)
        }

        parsedStructure.usernameId?.let {
            datasetBuilder.setValue(it,
                AutofillValue.forText(credential.username), usernamePresentation)
        }

        parsedStructure.passwordId?.let {
            datasetBuilder.setValue(it,
                AutofillValue.forText(credential.password), passwordPresentation)
        }

        // is this building a concatenated length of autofill_items?
        val dataset = datasetBuilder.build()
        return Pair<Dataset, String?>(dataset, credential.hostname)
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

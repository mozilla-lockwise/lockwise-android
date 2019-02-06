package mozilla.lockbox.autofill

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.R
import mozilla.lockbox.support.ParsedStructure
import mozilla.lockbox.view.AuthActivity
import java.net.URL

@TargetApi(Build.VERSION_CODES.O)
class FillResponseBuilder(
    private val parsedStructure: ParsedStructure,
    private val hostname: String
) {
    fun buildAuthenticationFillResponse(context: Context): FillResponse {
        val responseBuilder = FillResponse.Builder()

        val authPresentation = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            val callToAction = context.resources.getString(R.string.autofill_authenticate_cta)
            setTextViewText(R.id.presentationText, callToAction)
        }

        val sender = AuthActivity.getAuthIntentSender(context, this)
        val autofillIds = arrayOf(parsedStructure.usernameId, parsedStructure.passwordId)

        responseBuilder.setAuthentication(autofillIds, sender, authPresentation)

        return responseBuilder.build()
    }

    fun buildFallbackFillResponse(context: Context): FillResponse? {
        // See https://github.com/mozilla-lockbox/lockbox-android/issues/421
        val builder = FillResponse.Builder()
        addSearchFooter(context, builder)
        return builder.build()
    }

    private fun addSearchFooter(context: Context, builder: FillResponse.Builder) {
        val searchPresentation = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            val callToAction = context.resources.getString(R.string.autofill_search_cta)
            setTextViewText(R.id.presentationText, callToAction)
        }

        // See https://github.com/mozilla-lockbox/lockbox-android/issues/421
        val sender = AuthActivity.getAuthIntentSender(context, this)
        val autofillIds = arrayOf(parsedStructure.usernameId, parsedStructure.passwordId)

        builder.setAuthentication(autofillIds, sender, searchPresentation)
    }

    fun buildFilteredFillResponse(context: Context, passwords: List<ServerPassword>): FillResponse? {
        val myURL = URL(hostname)
        val possibleValues = passwords.filter {
            val credential = URL(it.hostname)
            credential.protocol == myURL.protocol &&
                (credential.host.endsWith(myURL.host, true) || myURL.host.endsWith(credential.host, true))
        }

        if (possibleValues.isEmpty()) {
            return null
        }

        val builder = FillResponse.Builder()

        possibleValues
            .map { serverPasswordToDataset(context, it) }
            .forEach { builder.addDataset(it) }

        addSearchFooter(context, builder)

        return builder.build()
    }

    private fun serverPasswordToDataset(
        context: Context,
        credential: ServerPassword
    ): Dataset {
        val datasetBuilder = Dataset.Builder()
        val usernamePresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
        val passwordPresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
        usernamePresentation.setTextViewText(mozilla.lockbox.R.id.presentationText, credential.username)
        passwordPresentation.setTextViewText(
            mozilla.lockbox.R.id.presentationText,
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

        return datasetBuilder.build()
    }
}

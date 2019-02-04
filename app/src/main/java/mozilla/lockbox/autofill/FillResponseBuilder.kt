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

@TargetApi(Build.VERSION_CODES.O)
class FillResponseBuilder(
    private val parsedStructure: ParsedStructure,
    private val webDomain: String
) {
    fun buildAuthenticationFillResponse(context: Context): FillResponse {
        val responseBuilder = FillResponse.Builder()

        val sender = AuthActivity.getAuthIntentSender(context, this)
        val autofillIds = arrayOf(parsedStructure.usernameId, parsedStructure.passwordId)

        val authPresentation = RemoteViews(context.packageName, R.layout.autofill_item).apply {
            setTextViewText(R.id.presentationText, "requires authentication")
        }

        responseBuilder.setAuthentication(autofillIds, sender, authPresentation)

        return responseBuilder.build()
    }

    fun buildFilteredFillResponse(context: Context, passwords: List<ServerPassword>): FillResponse? {
        val possibleValues = passwords.filter {
            it.hostname.contains(webDomain, true)
        }

        if (possibleValues.isEmpty()) {
            return null
        }

        val builder = FillResponse.Builder()

        possibleValues
            .map { serverPasswordToDataset(context, it) }
            .forEach { builder.addDataset(it) }

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

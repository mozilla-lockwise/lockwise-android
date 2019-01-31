package mozilla.lockbox.support

import android.R
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.service.autofill.Dataset
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import mozilla.appservices.logins.ServerPassword

@TargetApi(Build.VERSION_CODES.O)
fun serverPasswordToDataset(context: Context, parsedStructure: ParsedStructure, credential: ServerPassword): Dataset {
    val datasetBuilder = Dataset.Builder()
    val usernamePresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
    val passwordPresentation = RemoteViews(context.packageName, mozilla.lockbox.R.layout.autofill_item)
    usernamePresentation.setTextViewText(mozilla.lockbox.R.id.presentationText, credential.username)
    passwordPresentation.setTextViewText(
        mozilla.lockbox.R.id.presentationText,
        context.getString(mozilla.lockbox.R.string.password_for, credential.username)
    )

    parsedStructure.usernameId?.let {
        datasetBuilder.setValue(it, AutofillValue.forText(credential.username), usernamePresentation)
    }

    parsedStructure.passwordId?.let {
        datasetBuilder.setValue(it, AutofillValue.forText(credential.password), passwordPresentation)
    }

    return datasetBuilder.build()
}
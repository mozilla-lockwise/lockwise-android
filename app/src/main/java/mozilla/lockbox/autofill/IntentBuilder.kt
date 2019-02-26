/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.view.AutofillRootActivity

@ExperimentalCoroutinesApi
class IntentBuilder {
    companion object {
        lateinit var responseBuilder: FillResponseBuilder

        fun getAuthIntentSender(context: Context, responseBuilder: FillResponseBuilder): IntentSender {
            this.responseBuilder = responseBuilder
            val intent = Intent(context, AutofillRootActivity::class.java)
            // future work: add responsebuilder to intent via parcelable impl.

            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        fun getSearchIntentSender(context: Context, responseBuilder: FillResponseBuilder): IntentSender {
            this.responseBuilder = responseBuilder
            val intent = Intent(context, AutofillRootActivity::class.java)
            // future work: add responsebuilder to intent via parcelable impl.
            // add extras to the intent?
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}
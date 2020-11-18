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
import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.view.AutofillRootActivity

const val parsedStructureExtra = "parsedStructure"
const val searchRequiredExtra = "searchRequired"

@ExperimentalCoroutinesApi
class IntentBuilder {
    companion object {
        fun getAuthIntentSender(context: Context, responseBuilder: FillResponseBuilder): IntentSender {
            val intent = Intent(context, AutofillRootActivity::class.java)
            setResponseBuilder(intent, responseBuilder)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        fun getSearchIntentSender(context: Context, responseBuilder: FillResponseBuilder): IntentSender {
            val intent = Intent(context, AutofillRootActivity::class.java)
            setResponseBuilder(intent, responseBuilder)
            setSearchRequired(intent)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        fun setResponseBuilder(intent: Intent, responseBuilder: FillResponseBuilder) {
            val extras = Bundle()
            extras.putParcelable(parsedStructureExtra, responseBuilder.parsedStructure)
            intent.putExtra(parsedStructureExtra, extras)
        }

        fun getResponseBuilder(intent: Intent): FillResponseBuilder {
            val extras = intent.getBundleExtra(parsedStructureExtra)
            val parsedStructure = extras?.getParcelable<ParsedStructure>(parsedStructureExtra)
                ?: throw IllegalStateException("Unable to reconstruct parsedStructure")

            return FillResponseBuilder(parsedStructure)
        }

        fun isSearchRequired(intent: Intent): Boolean {
            return intent.getBooleanExtra(searchRequiredExtra, false)
        }

        fun setSearchRequired(intent: Intent, value: Boolean = true) {
            intent.putExtra(searchRequiredExtra, value)
        }
    }
}
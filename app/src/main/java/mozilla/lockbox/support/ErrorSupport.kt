/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.support

import android.text.TextUtils
import android.webkit.URLUtil
import com.google.android.material.textfield.TextInputLayout
import mozilla.lockbox.R
import mozilla.lockbox.action.SentryAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.log

private val dispatcher: Dispatcher = Dispatcher.shared

fun pushError(throwable: Throwable, message: String? = null) {
    log.error(message)
    dispatcher.dispatch(SentryAction(throwable))
}

fun validateEditTextAndShowError(inputLayout: TextInputLayout): String? {
    val inputText: String? = inputLayout.editText?.text.toString()

    val errorMessage: String? = when (inputLayout.id) {
        R.id.inputLayoutHostname -> {
            // hostname cannot be null
            // has to have http:// or https://
            when {
                TextUtils.isEmpty(inputText)
                    || !URLUtil.isHttpUrl(inputText)
                    || !URLUtil.isHttpsUrl(inputText)
                -> inputLayout.context.getString(R.string.hostname_invalid_text)
                else -> null
            }
        }
        R.id.inputLayoutPassword -> {
            // password cannot be empty
            // cannot be just spaces
            when {
                TextUtils.isEmpty(inputText) -> inputLayout.context.getString(R.string.password_invalid_text)
                else -> null
            }
        }
        else -> null // includes username
    }

    return errorMessage
}

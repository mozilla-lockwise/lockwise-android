/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import io.reactivex.Observable
import mozilla.lockbox.R

enum class AlertState {
    BUTTON_POSITIVE, BUTTON_NEGATIVE
}

object AlertDialogHelper {
    fun showAlertDialog(
        context: Context,
        @StringRes title: Int? = null,
        @StringRes message: Int? = null,
        @StringRes positiveButtonTitle: Int? = null,
        @StringRes negativeButtonTitle: Int? = null
    ): Observable<AlertState> {
        return Observable.create { emitter ->
            val builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)

            title?.let { builder.setTitle(title) }
            message?.let { builder.setMessage(message) }

            positiveButtonTitle?.let {
                builder.setPositiveButton(positiveButtonTitle) { _, _ ->
                    emitter.onNext(AlertState.BUTTON_POSITIVE)
                }
            }

            negativeButtonTitle?.let {
                builder.setNegativeButton(negativeButtonTitle) { _, _ ->
                    emitter.onNext(AlertState.BUTTON_NEGATIVE)
                }
            }

            builder.setOnDismissListener {
                // make sure to complete / dispose of observer when the dialog is no longer shown
                emitter.onComplete()
            }

            builder.show()
        }
    }
}

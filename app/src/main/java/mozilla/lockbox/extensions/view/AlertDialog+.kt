/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions.view

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import kotlinx.android.synthetic.main.list_cell_setting_toggle.*
import mozilla.lockbox.R
import mozilla.lockbox.model.DialogViewModel

enum class AlertState {
    BUTTON_POSITIVE, BUTTON_NEGATIVE
}

object AlertDialogHelper {
    fun showAlertDialog(
        context: Context,
        viewModel: DialogViewModel
    ): Observable<AlertState> {
        return Observable.create { emitter ->
            val builder = AlertDialog.Builder(context, R.style.DeleteDialogStyle)

            viewModel.title?.let {
                val titleString = context.getString(it)
                if (titleString.contains("%1\$s")) {
                    val appName = context.getString(R.string.app_name)
                    builder.setTitle(String.format(titleString, appName))
                } else {
                    builder.setTitle(it)
                }
            }

            viewModel.message?.let {
                val titleString = context.getString(it)
                if (titleString.contains("%1\$s")) {
                    val appName = context.getString(R.string.app_name)
                    builder.setMessage(String.format(titleString, appName))
                } else {
                    builder.setMessage(it)
                }
            }

            viewModel.positiveButtonTitle?.let {
                builder.setPositiveButton(it) { _, _ ->
                    emitter.onNext(AlertState.BUTTON_POSITIVE)
                }
            }

            viewModel.negativeButtonTitle?.let {
                builder.setNegativeButton(it) { _, _ ->
                    emitter.onNext(AlertState.BUTTON_NEGATIVE)
                }
            }

            setUpDismissal(builder, emitter)

            val dialog = builder.create()
            dialog.show()

            val defaultColor = context.getColor(R.color.violet_70)

            viewModel.positiveButtonColor?.let {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(context.getColor(it))
            } ?: run {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(defaultColor)
            }

            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(defaultColor)
        }
    }

    fun showRadioAlertDialog(
        context: Context,
        @StringRes title: Int? = null,
        items: Array<Int>,
        checkedItem: Int,
        @StringRes positiveButtonTitle: Int? = null,
        @StringRes negativeButtonTitle: Int? = null
    ): Observable<Int> {
        return Observable.create { emitter ->
            val builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)

            title?.let { builder.setTitle(it) }

            val stringItems = items.map { context.getString(it) }.toTypedArray()

            builder.setSingleChoiceItems(stringItems, checkedItem) { dialog, which ->
                emitter.onNext(which)
                dialog.dismiss()
            }

            positiveButtonTitle?.let {
                builder.setPositiveButton(positiveButtonTitle) { _, _ -> }
            }

            negativeButtonTitle?.let {
                builder.setNegativeButton(negativeButtonTitle) { _, _ -> }
            }

            setUpDismissal(builder, emitter)

            builder.show()
        }
    }

    private fun <T : Any> setUpDismissal(
        builder: AlertDialog.Builder,
        emitter: ObservableEmitter<T>
    ) {
        builder.setOnDismissListener {
            // make sure to complete / dispose of observer when the dialog is no longer shown
            emitter.onComplete()
        }
    }
}

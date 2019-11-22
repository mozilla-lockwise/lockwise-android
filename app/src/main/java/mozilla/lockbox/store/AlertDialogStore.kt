/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.DialogAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.Setting
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.extensions.view.AlertDialogHelper
import mozilla.lockbox.extensions.view.AlertState
import mozilla.lockbox.flux.Dispatcher

class AlertDialogStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {

    companion object {
        val shared: AlertDialogStore by lazy { AlertDialogStore() }
    }

    val compositeDisposable = CompositeDisposable()

    fun showDialog(activity: Activity, destination: DialogAction) {
        AlertDialogHelper.showAlertDialog(activity, destination.viewModel)
            .map { alertState ->
                when (alertState) {
                    AlertState.BUTTON_POSITIVE -> {
                        destination.positiveButtonActionList
                    }
                    AlertState.BUTTON_NEGATIVE -> {
                        destination.negativeButtonActionList
                    }
                }
            }
            .flatMapIterable { listOf(RouteAction.InternalBack) + it }
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }

    fun showAutoLockSelections(value: Setting.AutoLockTime, activity: AppCompatActivity) {
        val autoLockValues = Setting.AutoLockTime.values()
        val items = autoLockValues.map { it.stringValue }.toTypedArray()
        val index = autoLockValues.indexOf(value)

        AlertDialogHelper.showRadioAlertDialog(
            activity,
            R.string.auto_lock,
            items,
            index,
            negativeButtonTitle = R.string.cancel
        )
        .flatMapIterable {
            listOf(RouteAction.InternalBack, SettingAction.AutoLockTime(autoLockValues[it]))
        }
        .subscribe(dispatcher::dispatch)
        .addTo(compositeDisposable)
    }

    fun dismissDialogs() {
        compositeDisposable.clear()
    }
}

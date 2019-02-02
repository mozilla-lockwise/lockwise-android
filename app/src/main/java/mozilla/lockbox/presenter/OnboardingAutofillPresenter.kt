/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.annotation.TargetApi
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.lockbox.action.OnboardingStatusAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.log
import mozilla.lockbox.store.SettingStore

interface OnboardingAutofillView {
    val onDismiss: Observable<Unit>
    val onEnable: Observable<Unit>
}

@ExperimentalCoroutinesApi
class OnboardingAutofillPresenter(
    private val view: OnboardingAutofillView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewReady() {

        // if we can't autofill, just skip altogether
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dispatcher.dispatch(OnboardingStatusAction(false))
        }

        view.onDismiss.subscribe {
            dispatcher.dispatch(OnboardingStatusAction(false))
        }?.addTo(compositeDisposable)

        view.onEnable.subscribe {
            if(SettingStore.shared.autofillAvailable){
                log.error("ELISE TRUE ")
                dispatcher.dispatch(SettingAction.Autofill(true))
                dispatcher.dispatch(RouteAction.SystemSetting(SettingIntent.Autofill))
            }
            log.error("ELISE FALSE ")
            dispatcher.dispatch(OnboardingStatusAction(false))
        }?.addTo(compositeDisposable)
    }
}

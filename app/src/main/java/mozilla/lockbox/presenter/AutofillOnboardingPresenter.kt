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
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.SettingIntent
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import java.util.concurrent.TimeUnit

interface AutofillOnboardingView {
    val onSkipClick: Observable<Unit>
    val onGoToSettingsClick: Observable<Unit>
}

@TargetApi(Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalCoroutinesApi
class AutofillOnboardingPresenter(
    private val view: AutofillOnboardingView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {

    override fun onViewReady() {
        view.onSkipClick
            .subscribe {
                triggerNextOnboarding()
            }
            .addTo(compositeDisposable)

        view.onGoToSettingsClick
            .subscribe {
                dispatcher.dispatch(RouteAction.SystemSetting(SettingIntent.Autofill))
                triggerNextOnboarding(delayed = true)
            }
            .addTo(compositeDisposable)
    }

    private fun triggerNextOnboarding(delayed: Boolean = false) {
        val delayDuration: Long = if (delayed) 750 else 0
        Observable.just(Unit)
            .delay(delayDuration, TimeUnit.MILLISECONDS)
            .subscribe {
                dispatcher.dispatch(RouteAction.Onboarding.Confirmation)
            }
            .addTo(compositeDisposable)
    }
}

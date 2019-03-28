/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import java.util.concurrent.TimeUnit

interface AutofillFilterBackdropView

class AutofillFilterBackdropPresenter(
    val view: AutofillFilterBackdropView,
    private val dispatcher: Dispatcher = Dispatcher.shared
) : Presenter() {
    override fun onViewReady() {
        Observable.just(RouteAction.DialogFragment.AutofillSearchDialog)
            .delay(1, TimeUnit.SECONDS)
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)
    }
}
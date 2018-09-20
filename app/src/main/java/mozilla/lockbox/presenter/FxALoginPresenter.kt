/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

interface FxALoginViewProtocol {
    val logMeInClicks: Observable<Unit>
}

class FxALoginPresenter(private val protocol: FxALoginViewProtocol, private val dispatcher: Dispatcher = Dispatcher.shared) {

    private val compositeDisposable = CompositeDisposable()

    fun onViewReady() {
        this.protocol.logMeInClicks.subscribe {
            dispatcher.dispatch(RouteAction.ITEMLIST)
        }.addTo(compositeDisposable)
    }

    fun onDestroy() {
        compositeDisposable.clear()
    }
}
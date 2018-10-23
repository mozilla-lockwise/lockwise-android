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
import mozilla.lockbox.store.FingerprintStore

interface LockedView {
    val unlockButtonTaps: Observable<Unit>
    fun showFingerprintDialog()
}

class LockedPresenter(
    private val view: LockedView,
    private val dispatcher: Dispatcher = Dispatcher.shared,
    private val fingerprintStore: FingerprintStore = FingerprintStore.shared
) : Presenter() {
    override fun onViewReady() {
        view.unlockButtonTaps
            .subscribe {
                if (fingerprintStore.isFingerprintAuthAvailable()) {
                    view.showFingerprintDialog()
                } else {
                    dispatcher.dispatch(RouteAction.ItemList)
                }
            }
            .addTo(compositeDisposable)
    }
}

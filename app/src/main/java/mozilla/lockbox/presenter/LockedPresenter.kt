/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.presenter

import android.app.Activity.RESULT_OK
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import mozilla.lockbox.R
import mozilla.lockbox.action.AutofillAction
import mozilla.lockbox.action.DataStoreAction
import mozilla.lockbox.action.FingerprintAuthAction
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.UnlockingAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Action
import mozilla.lockbox.flux.Dispatcher
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.store.FingerprintStore
import mozilla.lockbox.store.LockedStore
import mozilla.lockbox.support.Constant
import java.util.concurrent.TimeUnit

interface LockedView {
    val onActivityResult: Observable<Pair<Int, Int>>
    val unlockButtonTaps: Observable<Unit>?
}

abstract class LockedPresenter(
    val lockedView: LockedView,
    open val dispatcher: Dispatcher,
    open val fingerprintStore: FingerprintStore,
    open val lockedStore: LockedStore
) : Presenter() {

    abstract fun Observable<Unit>.unlockAuthenticationObservable(): Observable<Boolean>

    override fun onViewReady() {
        Observable.just(Unit)
            .observeOn(AndroidSchedulers.mainThread())
            .unlockAuthenticationObservable()
            .delay(500, TimeUnit.MILLISECONDS)
            .map {
                if (fingerprintStore.isFingerprintAuthAvailable && it) {
                    RouteAction.DialogFragment.FingerprintDialog(R.string.fingerprint_dialog_title)
                } else if (fingerprintStore.isDeviceSecure) {
                    RouteAction.UnlockFallbackDialog
                } else {
                    // if there is no device security, unlock without any prompting
                    unlock()
                }
            }
            .filterByType(Action::class.java)
            .subscribe(dispatcher::dispatch)
            .addTo(compositeDisposable)

        lockedView.onActivityResult
            .subscribe {
                if (it.first == Constant.RequestCode.unlock && it.second == RESULT_OK) {
                    unlock()
                } else {
                    cancel()
                }
            }
            .addTo(compositeDisposable)

        lockedStore.onAuthentication
            .subscribe {
                when (it) {
                    is FingerprintAuthAction.OnSuccess -> unlock()
                    is FingerprintAuthAction.OnError ->
                        if (fingerprintStore.isKeyguardDeviceSecure)
                        // route presenter takes care of the unlockFallback()
                            dispatcher.dispatch(RouteAction.UnlockFallbackDialog)
                        else unlock()
                    is FingerprintAuthAction.OnCancel -> {
                        cancel()
                    }
                }
            }
            .addTo(compositeDisposable)
    }

    private fun unlock() {
        dispatcher.dispatch(DataStoreAction.Unlock)
        dispatcher.dispatch(UnlockingAction(false))
    }

    private fun cancel() {
        dispatcher.dispatch(AutofillAction.Cancel)
        dispatcher.dispatch(UnlockingAction(false))
    }
}
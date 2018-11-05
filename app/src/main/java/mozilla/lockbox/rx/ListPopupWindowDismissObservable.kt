/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:JvmName("RxListPopupWindow")
@file:JvmMultifileClass

package mozilla.lockbox.rx

import android.support.annotation.CheckResult
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import mozilla.lockbox.support.isOnUiThread

@CheckResult
fun ListPopupWindow.dismisses(): Observable<Unit> {
    return ListPopupWindowDismissObservable(this)
}

private class ListPopupWindowDismissObservable(
    private val view: ListPopupWindow
) : Observable<Unit>() {

    override fun subscribeActual(observer: Observer<in Unit>) {
        if (!isOnUiThread()) { return }
        val listener = Listener(view, observer)
        observer.onSubscribe(listener)
        view.setOnDismissListener(listener)
    }

    private class Listener(
        private val listPopupWindow: ListPopupWindow,
        private val observer: Observer<in Unit>
    ) : MainThreadDisposable(), PopupWindow.OnDismissListener {

        override fun onDismiss() {
            if (!isDisposed) {
                observer.onNext(Unit)
            }
        }

        override fun onDispose() {
            listPopupWindow.setOnDismissListener(null)
        }
    }
}
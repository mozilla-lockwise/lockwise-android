/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

@file:JvmName("RxListPopupWindow")
@file:JvmMultifileClass

package mozilla.lockbox.rx

import android.os.Looper
import android.support.annotation.CheckResult
import android.view.View
import android.widget.AdapterView
import android.widget.ListPopupWindow
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import mozilla.lockbox.support.isOnUiThread

@CheckResult
fun ListPopupWindow.itemClicks(): Observable<ListItem> {
    return ListPopupWindowItemClickObservable(this)
}

class ListItem(val parent: AdapterView<*>?, val view: View?, val position: Int, val id: Long) { }

private class ListPopupWindowItemClickObservable(
    private val view: ListPopupWindow
) : Observable<ListItem>() {

    override fun subscribeActual(observer: Observer<in ListItem>) {
        if (!isOnUiThread()) { return }

        val listener = Listener(view, observer)
        observer.onSubscribe(listener)
        view.setOnItemClickListener(listener)
    }

    private class Listener(
        private val popupWindow: ListPopupWindow,
        private val observer: Observer<in ListItem>
    ) : MainThreadDisposable(), AdapterView.OnItemClickListener {

        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (!isDisposed) {
                val item = ListItem(parent, view, position, id)
                observer.onNext(item)
            }
        }

        override fun onDispose() {
            popupWindow.setOnItemClickListener(null)
        }
    }
}
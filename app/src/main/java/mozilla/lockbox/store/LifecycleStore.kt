/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.store

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import mozilla.lockbox.action.LifecycleAction
import mozilla.lockbox.extensions.filterByType
import mozilla.lockbox.flux.Dispatcher

class LifecycleStore(
    val dispatcher: Dispatcher = Dispatcher.shared
) {
    companion object {
        val shared = LifecycleStore()
    }

    internal val compositeDisposable = CompositeDisposable()
    val lifecycleFilter: Observable<LifecycleAction> = PublishSubject.create()

    init {
        val subject = lifecycleFilter as PublishSubject
        dispatcher.register
            .filterByType(LifecycleAction::class.java)
            .subscribe(subject::onNext)
            .addTo(compositeDisposable)
    }
}
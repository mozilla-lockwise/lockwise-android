/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.flux

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class Dispatcher {
    companion object {
        val shared = Dispatcher()
    }

    private val actionSubject = PublishSubject.create<Action>()

    val register: Observable<Action> = this.actionSubject

    fun dispatch(action: Action) {
        this.actionSubject.onNext(action)
    }
}
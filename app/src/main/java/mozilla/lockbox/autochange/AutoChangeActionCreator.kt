/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.autochange

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mozilla.appservices.logins.ServerPassword
import java.util.concurrent.TimeUnit

interface AutoChangeHandler {
    val progress: Observable<AutoChangeState>

    fun invoke(): Observable<ServerPassword>
}

class DummyAutoChangeHandler(val item: ServerPassword) : AutoChangeHandler {
    override val progress: Observable<AutoChangeState> = PublishSubject.create()

    override fun invoke(): Observable<ServerPassword> {
        val newItem = item.copy(password = "password1")

        val output = PublishSubject.create<ServerPassword>()

        val progress = this.progress as PublishSubject

        Observable.fromIterable(AutoChangeState.happyPathSteps)
            .concatMap {
                Observable.just(it).delay(1000, TimeUnit.MILLISECONDS)
            }
            .doAfterNext {
                when (it) {
                    is AutoChangeState.PasswordChangeSuccessful -> {
                        output.onNext(newItem)
                        output.onComplete()
                    }
                    is AutoChangeState.LoggedOut -> {
                        progress.onComplete()
                    }
                }
            }
            .subscribe(progress)

        return output
    }
}

class AutoChangeActionCreator {
    companion object {
        val shared = AutoChangeActionCreator()
    }

    fun create(item: ServerPassword): AutoChangeHandler {
        return DummyAutoChangeHandler(item)
    }
}

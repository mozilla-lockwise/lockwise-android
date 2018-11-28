/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import io.reactivex.Observable
import mozilla.appservices.logins.ServerPassword
import mozilla.lockbox.LogProvider
import mozilla.lockbox.model.ItemViewModel
import mozilla.lockbox.support.Optional

fun <T : Any, U : T> Observable<T>.filterByType(clazz: Class<out U>): Observable<U> {
    return this.filter { t -> clazz.isInstance(t) }.map { t -> clazz.cast(t) }
}

fun <T : Any> Observable<T>.debug(): Observable<T> {
    return this
        .doOnSubscribe {
            LogProvider.log.info("subscribed")
        }
        .doOnNext {
            LogProvider.log.info("event: $it")
        }
        .doOnError {
            LogProvider.log.info("error: ${it.localizedMessage}")
        }
        .doOnDispose {
            LogProvider.log.info("disposed")
        }
}

fun <T : Any, U : Optional<T>> Observable<U>.filterNotNull(): Observable<T> {
    return this.filter { it.value != null }.map { it.value }
}

fun Observable<List<ServerPassword>>.mapToItemViewModelList(): Observable<List<ItemViewModel>> {
    return this.map { list -> list.map { it.toViewModel() } }
}

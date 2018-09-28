/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.extensions

import io.reactivex.Observable
import mozilla.lockbox.model.ItemViewModel
import org.mozilla.sync15.logins.ServerPassword

fun <T : Any, U : T> Observable<T>.filterByType(clazz: Class<out U>): Observable<U> {
    return this.filter { t -> clazz.isInstance(t) }.map { t -> clazz.cast(t) }
}

fun Observable<List<ServerPassword>>.mapToItemViewModelList(): Observable<List<ItemViewModel>> {
    return this.map { it.map { it.toViewModel() } }
}

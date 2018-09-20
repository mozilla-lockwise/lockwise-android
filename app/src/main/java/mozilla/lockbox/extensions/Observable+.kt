package mozilla.lockbox.extensions

import io.reactivex.Observable

fun <T : Any, U : T> Observable<T>.filterByType(clazz: Class<out U>): Observable<U> {
    return this.filter { t -> clazz.isInstance(t) }.map { t -> clazz.cast(t) }
}
